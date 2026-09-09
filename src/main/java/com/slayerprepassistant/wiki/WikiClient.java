package com.slayerprepassistant.wiki;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	private final OkHttpClient httpClient;
	private final Gson gson;
	private final Map<String, WikiResponse> revisionCache = new HashMap<>();
	private final Map<String, WikiResponse> renderedCache = new HashMap<>();
	private final Map<String, BufferedImage> imageCache = new HashMap<>();
	private final Map<String, Boolean> imageMissCache = new HashMap<>();
	private final Map<String, List<Consumer<WikiResponse>>> pendingRevisions = new HashMap<>();
	private final Map<String, List<Consumer<WikiResponse>>> pendingRendered = new HashMap<>();
	private final Map<String, List<Consumer<BufferedImage>>> pendingImages = new HashMap<>();

	public WikiClient(OkHttpClient httpClient, Gson gson)
	{
		this.httpClient = httpClient;
		this.gson = gson;
	}

	public void fetchRevisionText(String title, Consumer<WikiResponse> callback)
	{
		String key = cacheKey(title);
		WikiResponse cached;
		synchronized (this)
		{
			cached = revisionCache.get(key);
			if (cached == null && pendingRevisions.containsKey(key))
			{
				pendingRevisions.get(key).add(callback);
				return;
			}
			if (cached != null)
			{
				// Fall through and invoke the callback outside the monitor.
			}
			else
			{
				pendingRevisions.put(key, new ArrayList<>(java.util.Collections.singletonList(callback)));
			}
		}
		if (cached != null)
		{
			callback.accept(cached);
			return;
		}
		HttpUrl url = HttpUrl.parse(API_URL).newBuilder()
			.addQueryParameter("action", "query")
			.addQueryParameter("prop", "revisions")
			.addQueryParameter("rvprop", "ids|timestamp|content")
			.addQueryParameter("rvslots", "main")
			.addQueryParameter("titles", title)
			.addQueryParameter("redirects", "1")
			.addQueryParameter("format", "json")
			.build();
		Request request = new Request.Builder()
			.url(url)
			.header("User-Agent", USER_AGENT)
			.build();
		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				completeWikiResponse(pendingRevisions, revisionCache, key, new WikiResponse(WikiState.ERROR, null, e.getMessage()));
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody body = response.body())
				{
					if (!response.isSuccessful() || body == null)
					{
						completeWikiResponse(pendingRevisions, revisionCache, key, new WikiResponse(WikiState.ERROR, null, "Wiki returned HTTP " + response.code()));
						return;
					}
					completeWikiResponse(pendingRevisions, revisionCache, key, parseRevision(title, body.string()));
				}
			}
		});
	}

	public void fetchRenderedHtml(String title, Consumer<WikiResponse> callback)
	{
		String key = cacheKey(title);
		WikiResponse cached;
		synchronized (this)
		{
			cached = renderedCache.get(key);
			if (cached == null && pendingRendered.containsKey(key))
			{
				pendingRendered.get(key).add(callback);
				return;
			}
			if (cached != null)
			{
				// Fall through and invoke the callback outside the monitor.
			}
			else
			{
				pendingRendered.put(key, new ArrayList<>(java.util.Collections.singletonList(callback)));
			}
		}
		if (cached != null)
		{
			callback.accept(cached);
			return;
		}
		HttpUrl url = HttpUrl.parse(API_URL).newBuilder()
			.addQueryParameter("action", "parse")
			.addQueryParameter("page", title)
			.addQueryParameter("prop", "text|revid")
			.addQueryParameter("redirects", "1")
			.addQueryParameter("format", "json")
			.build();
		Request request = new Request.Builder()
			.url(url)
			.header("User-Agent", USER_AGENT)
			.build();
		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				completeWikiResponse(pendingRendered, renderedCache, key, new WikiResponse(WikiState.ERROR, null, e.getMessage()));
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody body = response.body())
				{
					if (!response.isSuccessful() || body == null)
					{
						completeWikiResponse(pendingRendered, renderedCache, key, new WikiResponse(WikiState.ERROR, null, "Wiki returned HTTP " + response.code()));
						return;
					}
					completeWikiResponse(pendingRendered, renderedCache, key, parseRendered(title, body.string()));
				}
			}
		});
	}

	public void fetchPageImage(String title, int size, Consumer<BufferedImage> callback)
	{
		String key = cacheKey(title) + "|" + size;
		BufferedImage cached;
		boolean cachedMiss;
		synchronized (this)
		{
			cached = imageCache.get(key);
			cachedMiss = imageMissCache.containsKey(key);
			if (cached == null && !cachedMiss && pendingImages.containsKey(key))
			{
				pendingImages.get(key).add(callback);
				return;
			}
			if (cached == null && !cachedMiss)
			{
				pendingImages.put(key, new ArrayList<>(java.util.Collections.singletonList(callback)));
			}
		}
		if (cached != null || cachedMiss)
		{
			callback.accept(cached);
			return;
		}
		HttpUrl url = HttpUrl.parse(API_URL).newBuilder()
			.addQueryParameter("action", "query")
			.addQueryParameter("prop", "pageimages")
			.addQueryParameter("piprop", "thumbnail")
			.addQueryParameter("pithumbsize", String.valueOf(size))
			.addQueryParameter("titles", title)
			.addQueryParameter("redirects", "1")
			.addQueryParameter("format", "json")
			.build();
		Request request = new Request.Builder()
			.url(url)
			.header("User-Agent", USER_AGENT)
			.build();
		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				completeImageResponse(key, null, false);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody body = response.body())
				{
					if (!response.isSuccessful() || body == null)
					{
						completeImageResponse(key, null, false);
						return;
					}
					String imageUrl = pageImageUrl(body.string());
					if (imageUrl.isEmpty())
					{
						completeImageResponse(key, null, true);
						return;
					}
					fetchImage(key, imageUrl);
				}
			}
		});
	}

	private void fetchImage(String key, String imageUrl)
	{
		Request request = new Request.Builder()
			.url(imageUrl)
			.header("User-Agent", USER_AGENT)
			.build();
		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				completeImageResponse(key, null, false);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException
			{
				try (ResponseBody body = response.body())
				{
					completeImageResponse(key, response.isSuccessful() && body != null ? ImageIO.read(body.byteStream()) : null, response.isSuccessful());
				}
			}
		});
	}

	private void completeWikiResponse(Map<String, List<Consumer<WikiResponse>>> pending, Map<String, WikiResponse> cache, String key, WikiResponse response)
	{
		List<Consumer<WikiResponse>> callbacks;
		synchronized (this)
		{
			if (isCacheable(response))
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
			&& (response.getState() == WikiState.READY || (response.getMessage() != null && response.getMessage().contains("not found")));
	}

	private void completeImageResponse(String key, BufferedImage image, boolean cacheMiss)
	{
		List<Consumer<BufferedImage>> callbacks;
		synchronized (this)
		{
			if (image != null)
			{
				imageCache.put(key, image);
			}
			else if (cacheMiss)
			{
				imageMissCache.put(key, true);
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

	private String cacheKey(String title)
	{
		return WikiTitles.cacheKey(title);
	}

	private WikiResponse parseRevision(String fallbackTitle, String json)
	{
		try
		{
			JsonObject root = gson.fromJson(json, JsonObject.class);
			JsonObject query = root.getAsJsonObject("query");
			JsonObject pages = query == null ? null : query.getAsJsonObject("pages");
			if (pages == null || pages.entrySet().isEmpty())
			{
				return new WikiResponse(WikiState.ERROR, null, "Wiki page not found");
			}
			JsonObject page = pages.entrySet().iterator().next().getValue().getAsJsonObject();
			if (page.has("missing"))
			{
				return new WikiResponse(WikiState.ERROR, null, "Wiki page not found");
			}
			String title = page.has("title") ? page.get("title").getAsString() : fallbackTitle;
			JsonArray revisions = page.getAsJsonArray("revisions");
			if (revisions == null || revisions.size() == 0)
			{
				return new WikiResponse(WikiState.ERROR, null, "Wiki revision not found");
			}
			JsonObject revision = revisions.get(0).getAsJsonObject();
			long revisionId = revision.has("revid") ? revision.get("revid").getAsLong() : 0;
			String text = revisionText(revision);
			return new WikiResponse(WikiState.READY, new WikiRevision(title, revisionId, Instant.now(), text), "");
		}
		catch (RuntimeException ex)
		{
			return new WikiResponse(WikiState.ERROR, null, "Unable to parse Wiki response");
		}
	}

	private String pageImageUrl(String json)
	{
		try
		{
			JsonObject root = gson.fromJson(json, JsonObject.class);
			JsonObject query = root.getAsJsonObject("query");
			JsonObject pages = query == null ? null : query.getAsJsonObject("pages");
			if (pages == null || pages.entrySet().isEmpty())
			{
				return "";
			}
			JsonObject page = pages.entrySet().iterator().next().getValue().getAsJsonObject();
			JsonObject thumbnail = page.getAsJsonObject("thumbnail");
			return thumbnail != null && thumbnail.has("source") ? thumbnail.get("source").getAsString() : "";
		}
		catch (RuntimeException ex)
		{
			return "";
		}
	}

	private WikiResponse parseRendered(String fallbackTitle, String json)
	{
		try
		{
			JsonObject root = gson.fromJson(json, JsonObject.class);
			JsonObject parse = root.getAsJsonObject("parse");
			if (parse == null)
			{
				return new WikiResponse(WikiState.ERROR, null, "Wiki page not found");
			}
			String title = parse.has("title") ? parse.get("title").getAsString() : fallbackTitle;
			long revisionId = parse.has("revid") ? parse.get("revid").getAsLong() : 0;
			JsonObject text = parse.getAsJsonObject("text");
			JsonElement html = text == null ? null : text.get("*");
			return html == null
				? new WikiResponse(WikiState.ERROR, null, "Wiki rendered HTML not found")
				: new WikiResponse(WikiState.READY, new WikiRevision(title, revisionId, Instant.now(), html.getAsString()), "");
		}
		catch (RuntimeException ex)
		{
			return new WikiResponse(WikiState.ERROR, null, "Unable to parse Wiki response");
		}
	}

	private String revisionText(JsonObject revision)
	{
		JsonObject slots = revision.getAsJsonObject("slots");
		JsonObject main = slots == null ? null : slots.getAsJsonObject("main");
		JsonElement text = main == null ? revision.get("*") : main.get("*");
		if (text == null && main != null)
		{
			text = main.get("content");
		}
		return text == null ? "" : text.getAsString();
	}
}
