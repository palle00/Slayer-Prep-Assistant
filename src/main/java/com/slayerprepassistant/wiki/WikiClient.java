package com.slayerprepassistant.wiki;

import com.google.gson.Gson;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class WikiClient
{
	public static final String USER_AGENT = "SlayerPrepAssistant-RuneLite/0.0.1";
	private static final String API_URL = "https://oldschool.runescape.wiki/api.php";
	private static final int MAX_TITLES_PER_QUERY = 50;
	private static final int REVISION_CACHE_SIZE = 128;
	private static final int RENDERED_CACHE_SIZE = 64;
	private static final int IMAGE_CACHE_SIZE = 128;
	private static final int IMAGE_MISS_CACHE_SIZE = 256;
	private static final int IMAGE_METADATA_BATCH_DELAY_MS = 12;

	private final OkHttpClient httpClient;
	private final WikiApiResponseParser responseParser;
	private final Map<String, WikiResponse> revisionCache = lruMap(REVISION_CACHE_SIZE);
	private final Map<String, WikiResponse> renderedCache = lruMap(RENDERED_CACHE_SIZE);
	private final Map<String, BufferedImage> imageCache = lruMap(IMAGE_CACHE_SIZE);
	private final Map<String, Boolean> imageMissCache = lruMap(IMAGE_MISS_CACHE_SIZE);
	private final Map<String, List<Consumer<WikiResponse>>> pendingRevisions = new LinkedHashMap<>();
	private final Map<String, List<Consumer<WikiResponse>>> pendingRendered = new LinkedHashMap<>();
	private final Map<String, List<Consumer<BufferedImage>>> pendingImages = new LinkedHashMap<>();
	private final Map<Integer, LinkedHashMap<String, String>> queuedImageTitlesBySize = new LinkedHashMap<>();
	private final ScheduledExecutorService imageBatchExecutor = Executors.newSingleThreadScheduledExecutor(runnable ->
	{
		Thread thread = new Thread(runnable, "slayer-prep-wiki-image-batch");
		thread.setDaemon(true);
		return thread;
	});
	private ScheduledFuture<?> imageBatchTask;
	private final Set<Call> activeCalls = Collections.newSetFromMap(new ConcurrentHashMap<Call, Boolean>());
	private volatile boolean closed;

	public WikiClient(OkHttpClient httpClient, Gson gson)
	{
		this.httpClient = httpClient;
		this.responseParser = new WikiApiResponseParser(gson);
	}

	public void fetchRevisionText(String title, Consumer<WikiResponse> callback)
	{
		fetchRevisionTexts(Collections.singletonList(title), responses ->
		{
			if (callback != null)
			{
				callback.accept(responses.getOrDefault(
					WikiTitles.cacheKey(title),
					new WikiResponse(WikiState.ERROR, null, "Wiki page not found")));
			}
		});
	}

	public void fetchRevisionTexts(List<String> titles, Consumer<Map<String, WikiResponse>> callback)
	{
		if (callback == null)
		{
			return;
		}
		Map<String, String> requestedTitles = distinctTitles(titles);
		if (requestedTitles.isEmpty())
		{
			callback.accept(Collections.emptyMap());
			return;
		}
		if (closed)
		{
			callback.accept(errorResponses(requestedTitles, "Wiki client is closed"));
			return;
		}

		Map<String, WikiResponse> collected = new ConcurrentHashMap<>();
		AtomicInteger remaining = new AtomicInteger(requestedTitles.size());
		Consumer<KeyedWikiResponse> collector = keyed ->
		{
			collected.put(keyed.key, keyed.response);
			if (remaining.decrementAndGet() == 0)
			{
				Map<String, WikiResponse> ordered = new LinkedHashMap<>();
				for (String key : requestedTitles.keySet())
				{
					ordered.put(key, collected.getOrDefault(key, new WikiResponse(WikiState.ERROR, null, "Wiki page not found")));
				}
				callback.accept(Collections.unmodifiableMap(ordered));
			}
		};

		List<String> freshTitles = new ArrayList<>();
		List<KeyedWikiResponse> cachedResponses = new ArrayList<>();
		synchronized (this)
		{
			for (Map.Entry<String, String> entry : requestedTitles.entrySet())
			{
				String key = entry.getKey();
				WikiResponse cached = revisionCache.get(key);
				if (cached != null)
				{
					cachedResponses.add(new KeyedWikiResponse(key, cached));
					continue;
				}

				Consumer<WikiResponse> perTitleCallback = response -> collector.accept(new KeyedWikiResponse(key, response));
				List<Consumer<WikiResponse>> pending = pendingRevisions.get(key);
				if (pending != null)
				{
					pending.add(perTitleCallback);
				}
				else
				{
					pendingRevisions.put(key, new ArrayList<>(Collections.singletonList(perTitleCallback)));
					freshTitles.add(entry.getValue());
				}
			}
		}

		for (KeyedWikiResponse cached : cachedResponses)
		{
			collector.accept(cached);
		}
		for (int start = 0; start < freshTitles.size(); start += MAX_TITLES_PER_QUERY)
		{
			int end = Math.min(freshTitles.size(), start + MAX_TITLES_PER_QUERY);
			enqueueRevisionBatch(new ArrayList<>(freshTitles.subList(start, end)));
		}
	}

	public void fetchRenderedHtml(String title, Consumer<WikiResponse> callback)
	{
		if (callback == null)
		{
			return;
		}
		if (closed)
		{
			callback.accept(new WikiResponse(WikiState.ERROR, null, "Wiki client is closed"));
			return;
		}
		String key = WikiTitles.cacheKey(title);
		WikiResponse cached;
		synchronized (this)
		{
			cached = renderedCache.get(key);
			if (cached == null && pendingRendered.containsKey(key))
			{
				pendingRendered.get(key).add(callback);
				return;
			}
			if (cached == null)
			{
				pendingRendered.put(key, new ArrayList<>(Collections.singletonList(callback)));
			}
		}
		if (cached != null)
		{
			callback.accept(cached);
			return;
		}

		HttpUrl url = apiUrlBuilder()
			.addQueryParameter("action", "parse")
			.addQueryParameter("page", title)
			.addQueryParameter("prop", "text|revid")
			.addQueryParameter("redirects", "1")
			.addQueryParameter("format", "json")
			.build();
		Call call = httpClient.newCall(request(url));
		if (!track(call))
		{
			return;
		}
		call.enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				untrack(call);
				completeWikiResponse(pendingRendered, renderedCache, key, error(e));
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				untrack(call);
				try (ResponseBody body = response.body())
				{
					if (!response.isSuccessful() || body == null)
					{
						completeWikiResponse(pendingRendered, renderedCache, key, httpError(response.code()));
						return;
					}
					completeWikiResponse(pendingRendered, renderedCache, key, responseParser.parseRendered(title, body.string()));
				}
			}
		});
	}

	public void fetchPageImage(String title, int size, Consumer<BufferedImage> callback)
	{
		if (callback == null)
		{
			return;
		}
		if (closed || title == null || title.trim().isEmpty() || size <= 0)
		{
			callback.accept(null);
			return;
		}

		String cleanedTitle = title.trim();
		String titleKey = WikiTitles.cacheKey(cleanedTitle);
		String key = titleKey + "|" + size;
		BufferedImage cached;
		boolean cachedMiss;
		boolean newRequest = false;
		synchronized (this)
		{
			cached = imageCache.get(key);
			cachedMiss = imageMissCache.containsKey(key);
			if (cached == null && !cachedMiss)
			{
				List<Consumer<BufferedImage>> pending = pendingImages.get(key);
				if (pending != null)
				{
					pending.add(callback);
					return;
				}
				pendingImages.put(key, new ArrayList<>(Collections.singletonList(callback)));
				queuedImageTitlesBySize.computeIfAbsent(size, ignored -> new LinkedHashMap<>()).put(titleKey, cleanedTitle);
				newRequest = true;
			}
		}
		if (cached != null || cachedMiss)
		{
			callback.accept(cached);
			return;
		}
		if (newRequest)
		{
			scheduleImageMetadataFlush();
		}
	}

	void executeAsync(Runnable task)
	{
		if (task == null || closed)
		{
			return;
		}
		httpClient.dispatcher().executorService().execute(task);
	}

	public void cancelOutstanding()
	{
		closed = true;
		synchronized (this)
		{
			if (imageBatchTask != null)
			{
				imageBatchTask.cancel(false);
				imageBatchTask = null;
			}
			queuedImageTitlesBySize.clear();
			pendingRevisions.clear();
			pendingRendered.clear();
			pendingImages.clear();
			revisionCache.clear();
			renderedCache.clear();
			imageCache.clear();
			imageMissCache.clear();
		}
		imageBatchExecutor.shutdownNow();
		for (Call call : new ArrayList<>(activeCalls))
		{
			call.cancel();
		}
		activeCalls.clear();
	}

	private void scheduleImageMetadataFlush()
	{
		synchronized (this)
		{
			if (closed || (imageBatchTask != null && !imageBatchTask.isDone()))
			{
				return;
			}
			try
			{
				imageBatchTask = imageBatchExecutor.schedule(this::flushImageMetadata, IMAGE_METADATA_BATCH_DELAY_MS, TimeUnit.MILLISECONDS);
			}
			catch (RejectedExecutionException ignored)
			{
				imageBatchTask = null;
			}
		}
	}

	private void flushImageMetadata()
	{
		Map<Integer, LinkedHashMap<String, String>> queued;
		synchronized (this)
		{
			imageBatchTask = null;
			if (closed || queuedImageTitlesBySize.isEmpty())
			{
				return;
			}
			queued = new LinkedHashMap<>();
			for (Map.Entry<Integer, LinkedHashMap<String, String>> entry : queuedImageTitlesBySize.entrySet())
			{
				queued.put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
			}
			queuedImageTitlesBySize.clear();
		}

		for (Map.Entry<Integer, LinkedHashMap<String, String>> entry : queued.entrySet())
		{
			List<Map.Entry<String, String>> requests = new ArrayList<>(entry.getValue().entrySet());
			for (int start = 0; start < requests.size(); start += MAX_TITLES_PER_QUERY)
			{
				int end = Math.min(requests.size(), start + MAX_TITLES_PER_QUERY);
				enqueueImageMetadataBatch(entry.getKey(), new ArrayList<>(requests.subList(start, end)));
			}
		}
	}

	private void enqueueImageMetadataBatch(int size, List<Map.Entry<String, String>> requests)
	{
		if (closed || requests == null || requests.isEmpty())
		{
			return;
		}
		List<String> titles = new ArrayList<>(requests.size());
		for (Map.Entry<String, String> request : requests)
		{
			titles.add(request.getValue());
		}

		HttpUrl url = apiUrlBuilder()
			.addQueryParameter("action", "query")
			.addQueryParameter("prop", "pageimages")
			.addQueryParameter("piprop", "thumbnail")
			.addQueryParameter("pithumbsize", String.valueOf(size))
			.addQueryParameter("titles", String.join("|", titles))
			.addQueryParameter("redirects", "1")
			.addQueryParameter("format", "json")
			.build();
		Call call = httpClient.newCall(request(url));
		if (!track(call))
		{
			return;
		}
		call.enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				untrack(call);
				for (Map.Entry<String, String> request : requests)
				{
					completeImageResponse(request.getKey() + "|" + size, null, false);
				}
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				untrack(call);
				try (ResponseBody body = response.body())
				{
					if (!response.isSuccessful() || body == null)
					{
						for (Map.Entry<String, String> request : requests)
						{
							completeImageResponse(request.getKey() + "|" + size, null, false);
						}
						return;
					}

					Map<String, String> imageUrls = responseParser.pageImageUrls(titles, body.string());
					for (Map.Entry<String, String> request : requests)
					{
						String key = request.getKey() + "|" + size;
						String imageUrl = imageUrls.getOrDefault(request.getKey(), "");
						if (imageUrl.isEmpty())
						{
							completeImageResponse(key, null, true);
						}
						else
						{
							fetchImage(key, imageUrl);
						}
					}
				}
			}
		});
	}

	private void enqueueRevisionBatch(List<String> titles)
	{
		if (titles == null || titles.isEmpty())
		{
			return;
		}
		HttpUrl url = apiUrlBuilder()
			.addQueryParameter("action", "query")
			.addQueryParameter("prop", "revisions")
			.addQueryParameter("rvprop", "ids|content")
			.addQueryParameter("rvslots", "main")
			.addQueryParameter("titles", String.join("|", titles))
			.addQueryParameter("redirects", "1")
			.addQueryParameter("format", "json")
			.build();
		Call call = httpClient.newCall(request(url));
		if (!track(call))
		{
			return;
		}
		call.enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				untrack(call);
				WikiResponse failure = error(e);
				for (String title : titles)
				{
					completeWikiResponse(pendingRevisions, revisionCache, WikiTitles.cacheKey(title), failure);
				}
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				untrack(call);
				try (ResponseBody body = response.body())
				{
					if (!response.isSuccessful() || body == null)
					{
						WikiResponse failure = httpError(response.code());
						for (String title : titles)
						{
							completeWikiResponse(pendingRevisions, revisionCache, WikiTitles.cacheKey(title), failure);
						}
						return;
					}
					Map<String, WikiResponse> parsed = responseParser.parseRevisions(titles, body.string());
					for (String title : titles)
					{
						String key = WikiTitles.cacheKey(title);
						completeWikiResponse(pendingRevisions, revisionCache, key,
							parsed.getOrDefault(key, new WikiResponse(WikiState.ERROR, null, "Wiki page not found")));
					}
				}
			}
		});
	}

	private void fetchImage(String key, String imageUrl)
	{
		if (closed)
		{
			completeImageResponse(key, null, false);
			return;
		}
		Call call = httpClient.newCall(new Request.Builder().url(imageUrl).header("User-Agent", USER_AGENT).build());
		if (!track(call))
		{
			return;
		}
		call.enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				untrack(call);
				completeImageResponse(key, null, false);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				untrack(call);
				try (ResponseBody body = response.body())
				{
					BufferedImage image = response.isSuccessful() && body != null ? ImageIO.read(body.byteStream()) : null;
					completeImageResponse(key, image, response.isSuccessful());
				}
			}
		});
	}

	private void completeWikiResponse(
		Map<String, List<Consumer<WikiResponse>>> pending,
		Map<String, WikiResponse> cache,
		String key,
		WikiResponse response)
	{
		List<Consumer<WikiResponse>> callbacks;
		synchronized (this)
		{
			if (!closed && isCacheable(response))
			{
				cache.put(key, response);
			}
			callbacks = pending.remove(key);
		}
		if (callbacks != null)
		{
			for (Consumer<WikiResponse> callback : callbacks)
			{
				callback.accept(response);
			}
		}
	}

	private boolean isCacheable(WikiResponse response)
	{
		return response != null
			&& (response.getState() == WikiState.READY
				|| (response.getMessage() != null && response.getMessage().toLowerCase(java.util.Locale.ROOT).contains("not found")));
	}

	private void completeImageResponse(String key, BufferedImage image, boolean cacheMiss)
	{
		List<Consumer<BufferedImage>> callbacks;
		synchronized (this)
		{
			if (!closed)
			{
				if (image != null)
				{
					imageCache.put(key, image);
				}
				else if (cacheMiss)
				{
					imageMissCache.put(key, true);
				}
			}
			callbacks = pendingImages.remove(key);
		}
		if (callbacks != null)
		{
			for (Consumer<BufferedImage> callback : callbacks)
			{
				callback.accept(image);
			}
		}
	}

	private Request request(HttpUrl url)
	{
		return new Request.Builder().url(url).header("User-Agent", USER_AGENT).build();
	}

	private HttpUrl.Builder apiUrlBuilder()
	{
		HttpUrl apiUrl = HttpUrl.parse(API_URL);
		if (apiUrl == null)
		{
			throw new IllegalStateException("Invalid Wiki API URL");
		}
		return apiUrl.newBuilder();
	}

	private boolean track(Call call)
	{
		if (call == null || closed)
		{
			if (call != null)
			{
				call.cancel();
			}
			return false;
		}
		activeCalls.add(call);
		if (closed && activeCalls.remove(call))
		{
			call.cancel();
			return false;
		}
		return true;
	}

	private void untrack(Call call)
	{
		activeCalls.remove(call);
	}

	private WikiResponse error(IOException e)
	{
		return new WikiResponse(WikiState.ERROR, null, e == null || e.getMessage() == null ? "Wiki request failed" : e.getMessage());
	}

	private WikiResponse httpError(int code)
	{
		return new WikiResponse(WikiState.ERROR, null, "Wiki returned HTTP " + code);
	}

	private Map<String, String> distinctTitles(List<String> titles)
	{
		Map<String, String> result = new LinkedHashMap<>();
		if (titles == null)
		{
			return result;
		}
		for (String title : titles)
		{
			if (title == null || title.trim().isEmpty())
			{
				continue;
			}
			String cleaned = title.trim();
			result.putIfAbsent(WikiTitles.cacheKey(cleaned), cleaned);
		}
		return result;
	}

	private Map<String, WikiResponse> errorResponses(Map<String, String> requestedTitles, String message)
	{
		Map<String, WikiResponse> result = new LinkedHashMap<>();
		for (String key : requestedTitles.keySet())
		{
			result.put(key, new WikiResponse(WikiState.ERROR, null, message));
		}
		return result;
	}

	private static <K, V> Map<K, V> lruMap(int maximumSize)
	{
		return new LinkedHashMap<K, V>(16, 0.75f, true)
		{
			@Override
			protected boolean removeEldestEntry(Map.Entry<K, V> eldest)
			{
				return size() > maximumSize;
			}
		};
	}

	private static final class KeyedWikiResponse
	{
		private final String key;
		private final WikiResponse response;

		private KeyedWikiResponse(String key, WikiResponse response)
		{
			this.key = key;
			this.response = response == null ? new WikiResponse(WikiState.ERROR, null, "Wiki request failed") : response;
		}
	}
}
