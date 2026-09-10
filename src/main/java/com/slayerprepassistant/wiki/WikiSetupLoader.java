package com.slayerprepassistant.wiki;

import com.slayerprepassistant.task.TargetOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class WikiSetupLoader
{
	private static final int PARSED_CACHE_SIZE = 128;

	private final WikiClient wikiClient;
	private final WikiStrategyParser wikiStrategyParser;
	private final java.util.function.Consumer<Runnable> clientThreadInvoker;
	private final Map<String, WikiParsingResult> wikiResultCache = Collections.synchronizedMap(
		new LinkedHashMap<String, WikiParsingResult>(16, 0.75f, true)
		{
			@Override
			protected boolean removeEldestEntry(Map.Entry<String, WikiParsingResult> eldest)
			{
				return size() > PARSED_CACHE_SIZE;
			}
		});

	public WikiSetupLoader(WikiClient wikiClient, WikiStrategyParser wikiStrategyParser, java.util.function.Consumer<Runnable> clientThreadInvoker)
	{
		this.wikiClient = Objects.requireNonNull(wikiClient, "wikiClient cannot be null");
		this.wikiStrategyParser = Objects.requireNonNull(wikiStrategyParser, "wikiStrategyParser cannot be null");
		this.clientThreadInvoker = Objects.requireNonNull(clientThreadInvoker, "clientThreadInvoker cannot be null");
	}

	public void loadStrategy(int requestId, TargetOption target, List<String> candidates, StaleCheck staleCheck, StrategyHandler handler, Runnable exhausted)
	{
		loadStrategy(requestId, target, target, candidates, staleCheck, handler, exhausted);
	}

	/**
	 * Loads the first usable strategy for {@code parseTarget} while staleness is checked against
	 * {@code staleTarget}. Candidate revision texts are fetched in a single MediaWiki batch request.
	 */
	public void loadStrategy(
		int requestId,
		TargetOption staleTarget,
		TargetOption parseTarget,
		List<String> candidates,
		StaleCheck staleCheck,
		StrategyHandler handler,
		Runnable exhausted)
	{
		List<String> safeCandidates = safeCandidates(candidates);
		Runnable safeExhausted = safeRunnable(exhausted);
		if (safeCandidates.isEmpty())
		{
			safeExhausted.run();
			return;
		}
		if (isStale(staleCheck, requestId, staleTarget))
		{
			return;
		}

		Map<String, WikiParsingResult> cached = cachedResults(safeCandidates);
		if (cached.size() == safeCandidates.size())
		{
			continueStrategy(requestId, staleTarget, safeCandidates, cached, 0, staleCheck, handler, safeExhausted);
			return;
		}

		List<String> uncached = new ArrayList<>();
		for (String title : safeCandidates)
		{
			if (!cached.containsKey(WikiTitles.cacheKey(title)))
			{
				uncached.add(title);
			}
		}

		wikiClient.fetchRevisionTexts(uncached, responses -> wikiClient.executeAsync(() ->
		{
			Map<String, WikiParsingResult> parsed = new LinkedHashMap<>(cached);
			for (String title : uncached)
			{
				WikiResponse response = responses.get(WikiTitles.cacheKey(title));
				if (isReady(response))
				{
					parsed.put(WikiTitles.cacheKey(title), parse(parseTarget, response));
				}
			}
			clientThreadInvoker.accept(() ->
			{
				if (!isStale(staleCheck, requestId, staleTarget))
				{
					continueStrategy(requestId, staleTarget, safeCandidates, parsed, 0, staleCheck, handler, safeExhausted);
				}
			});
		}));
	}

	public void loadVariantPage(int requestId, TargetOption target, List<String> candidates, StaleCheck staleCheck, VariantHandler handler, Runnable exhausted)
	{
		List<String> safeCandidates = safeCandidates(candidates);
		Runnable safeExhausted = safeRunnable(exhausted);
		if (safeCandidates.isEmpty())
		{
			safeExhausted.run();
			return;
		}
		if (isStale(staleCheck, requestId, target))
		{
			return;
		}

		Map<String, WikiParsingResult> cached = cachedResults(safeCandidates);
		if (cached.size() == safeCandidates.size())
		{
			continueVariantPage(requestId, target, safeCandidates, cached, 0, staleCheck, handler, safeExhausted);
			return;
		}

		List<String> uncached = new ArrayList<>();
		for (String title : safeCandidates)
		{
			if (!cached.containsKey(WikiTitles.cacheKey(title)))
			{
				uncached.add(title);
			}
		}
		wikiClient.fetchRevisionTexts(uncached, responses -> wikiClient.executeAsync(() ->
		{
			Map<String, WikiParsingResult> parsed = new LinkedHashMap<>(cached);
			for (String title : uncached)
			{
				WikiResponse response = responses.get(WikiTitles.cacheKey(title));
				if (isReady(response))
				{
					WikiParsingResult result = parse(target, response);
					parsed.put(WikiTitles.cacheKey(title), result);
					cache(title, result);
				}
			}
			clientThreadInvoker.accept(() ->
			{
				if (!isStale(staleCheck, requestId, target))
				{
					continueVariantPage(requestId, target, safeCandidates, parsed, 0, staleCheck, handler, safeExhausted);
				}
			});
		}));
	}

	private void continueStrategy(
		int requestId,
		TargetOption staleTarget,
		List<String> candidates,
		Map<String, WikiParsingResult> parsedByTitle,
		int index,
		StaleCheck staleCheck,
		StrategyHandler handler,
		Runnable exhausted)
	{
		if (isStale(staleCheck, requestId, staleTarget))
		{
			return;
		}
		if (index >= candidates.size())
		{
			exhausted.run();
			return;
		}

		String title = candidates.get(index);
		String key = WikiTitles.cacheKey(title);
		WikiParsingResult cached = cachedResult(key);
		if (cached != null)
		{
			handleStrategyResult(requestId, staleTarget, candidates, parsedByTitle, index, staleCheck, handler, exhausted, cached);
			return;
		}

		WikiParsingResult parsed = parsedByTitle.get(key);
		if (parsed == null || parsed.getGuide() == null)
		{
			continueStrategy(requestId, staleTarget, candidates, parsedByTitle, index + 1, staleCheck, handler, exhausted);
			return;
		}

		String renderedTitle = parsed.getGuide().getWikiTitle();
		wikiClient.fetchRenderedHtml(renderedTitle, htmlResponse -> wikiClient.executeAsync(() ->
		{
			WikiParsingResult enriched = isReady(htmlResponse)
				? wikiStrategyParser.mergeRenderedHtml(parsed, htmlResponse.getRevision().getText())
				: parsed;
			cache(title, enriched);
			clientThreadInvoker.accept(() ->
			{
				if (!isStale(staleCheck, requestId, staleTarget))
				{
					handleStrategyResult(requestId, staleTarget, candidates, parsedByTitle, index, staleCheck, handler, exhausted, enriched);
				}
			});
		}));
	}

	private void handleStrategyResult(
		int requestId,
		TargetOption staleTarget,
		List<String> candidates,
		Map<String, WikiParsingResult> parsedByTitle,
		int index,
		StaleCheck staleCheck,
		StrategyHandler handler,
		Runnable exhausted,
		WikiParsingResult result)
	{
		boolean finalCandidate = index == candidates.size() - 1;
		boolean handled = handler != null && handler.handle(result, finalCandidate);
		if (!handled)
		{
			continueStrategy(requestId, staleTarget, candidates, parsedByTitle, index + 1, staleCheck, handler, exhausted);
		}
	}

	private void continueVariantPage(
		int requestId,
		TargetOption target,
		List<String> candidates,
		Map<String, WikiParsingResult> parsedByTitle,
		int index,
		StaleCheck staleCheck,
		VariantHandler handler,
		Runnable exhausted)
	{
		if (isStale(staleCheck, requestId, target))
		{
			return;
		}
		if (index >= candidates.size())
		{
			exhausted.run();
			return;
		}

		String title = candidates.get(index);
		WikiParsingResult parsed = parsedByTitle.get(WikiTitles.cacheKey(title));
		if (parsed != null)
		{
			boolean handled = handler != null && handler.handle(parsed);
			if (handled)
			{
				return;
			}
		}
		continueVariantPage(requestId, target, candidates, parsedByTitle, index + 1, staleCheck, handler, exhausted);
	}

	private Map<String, WikiParsingResult> cachedResults(List<String> candidates)
	{
		Map<String, WikiParsingResult> result = new LinkedHashMap<>();
		synchronized (wikiResultCache)
		{
			for (String title : candidates)
			{
				String key = WikiTitles.cacheKey(title);
				WikiParsingResult cached = wikiResultCache.get(key);
				if (cached != null)
				{
					result.put(key, cached);
				}
			}
		}
		return result;
	}

	private WikiParsingResult cachedResult(String key)
	{
		synchronized (wikiResultCache)
		{
			return wikiResultCache.get(key);
		}
	}

	private void cache(String title, WikiParsingResult result)
	{
		if (result == null)
		{
			return;
		}
		synchronized (wikiResultCache)
		{
			wikiResultCache.put(WikiTitles.cacheKey(title), result);
		}
	}

	private WikiParsingResult parse(TargetOption target, WikiResponse response)
	{
		String name = target != null ? target.getDisplayName() : "";
		String title = response.getRevision().getTitle();
		return wikiStrategyParser.parse(
			name,
			title,
			WikiTitles.pageUrl(title),
			response.getRevision().getRevisionId(),
			response.getRevision().getText());
	}

	private boolean isReady(WikiResponse response)
	{
		return response != null && response.getState() == WikiState.READY && response.getRevision() != null;
	}

	private boolean isStale(StaleCheck staleCheck, int requestId, TargetOption target)
	{
		return staleCheck != null && staleCheck.isStale(requestId, target);
	}

	private List<String> safeCandidates(List<String> candidates)
	{
		if (candidates == null || candidates.isEmpty())
		{
			return Collections.emptyList();
		}
		List<String> result = new ArrayList<>();
		Set<String> seen = new LinkedHashSet<>();
		for (String candidate : candidates)
		{
			if (candidate == null || candidate.trim().isEmpty())
			{
				continue;
			}
			String cleaned = candidate.trim();
			if (seen.add(WikiTitles.cacheKey(cleaned)))
			{
				result.add(cleaned);
			}
		}
		return Collections.unmodifiableList(result);
	}

	private Runnable safeRunnable(Runnable runnable)
	{
		return runnable != null ? runnable : () -> { };
	}

	public interface StaleCheck
	{
		boolean isStale(int requestId, TargetOption target);
	}

	public interface StrategyHandler
	{
		boolean handle(WikiParsingResult parsed, boolean finalCandidate);
	}

	public interface VariantHandler
	{
		boolean handle(WikiParsingResult parsed);
	}
}
