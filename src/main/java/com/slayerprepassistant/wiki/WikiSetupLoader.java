package com.slayerprepassistant.wiki;

import com.slayerprepassistant.task.TargetOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class WikiSetupLoader
{
	private final WikiClient wikiClient;
	private final WikiStrategyParser wikiStrategyParser;
	private final Consumer<Runnable> clientThreadInvoker;
	private final Map<String, WikiParsingResult> wikiResultCache = new ConcurrentHashMap<>();

	public WikiSetupLoader(WikiClient wikiClient, WikiStrategyParser wikiStrategyParser, Consumer<Runnable> clientThreadInvoker)
	{
		this.wikiClient = Objects.requireNonNull(wikiClient, "wikiClient cannot be null");
		this.wikiStrategyParser = Objects.requireNonNull(wikiStrategyParser, "wikiStrategyParser cannot be null");
		this.clientThreadInvoker = Objects.requireNonNull(clientThreadInvoker, "clientThreadInvoker cannot be null");
	}

	public void loadStrategy(int requestId, TargetOption target, List<String> candidates, StaleCheck staleCheck, StrategyHandler handler, Runnable exhausted)
	{
		loadStrategy(requestId, target, safeCandidates(candidates), 0, staleCheck, handler, safeRunnable(exhausted));
	}

	public void loadVariantPage(int requestId, TargetOption target, List<String> candidates, StaleCheck staleCheck, VariantHandler handler, Runnable exhausted)
	{
		loadVariantPage(requestId, target, safeCandidates(candidates), 0, staleCheck, handler, safeRunnable(exhausted));
	}

	public void loadStrategyCandidate(int requestId, TargetOption staleTarget, TargetOption parseTarget, String strategyTitle, StaleCheck staleCheck, Consumer<WikiParsingResult> callback)
	{
		if (strategyTitle == null || strategyTitle.trim().isEmpty() || callback == null)
		{
			if (callback != null)
			{
				callback.accept(null);
			}
			return;
		}

		String titleKey = strategyTitle.trim();
		WikiParsingResult cached = wikiResultCache.get(titleKey);
		if (cached != null)
		{
			if (staleCheck != null && staleCheck.isStale(requestId, staleTarget))
			{
				return;
			}
			callback.accept(cached);
			return;
		}

		wikiClient.fetchRevisionText(titleKey, response ->
				clientThreadInvoker.accept(() -> handleStrategyCandidateResponse(requestId, staleTarget, parseTarget, titleKey, staleCheck, callback, response))
		);
	}

	private void loadStrategy(int requestId, TargetOption target, List<String> candidates, int index, StaleCheck staleCheck, StrategyHandler handler, Runnable exhausted)
	{
		if (index >= candidates.size())
		{
			exhausted.run();
			return;
		}
		String strategyTitle = candidates.get(index);
		WikiParsingResult cached = wikiResultCache.get(strategyTitle);
		if (cached != null)
		{
			boolean handled = handler != null && handler.handle(cached, index == candidates.size() - 1);
			if (!handled)
			{
				loadStrategy(requestId, target, candidates, index + 1, staleCheck, handler, exhausted);
			}
			return;
		}
		wikiClient.fetchRevisionText(strategyTitle, response ->
				clientThreadInvoker.accept(() -> handleStrategyResponse(requestId, target, candidates, index, staleCheck, handler, exhausted, response))
		);
	}

	private void handleStrategyResponse(int requestId, TargetOption target, List<String> candidates, int index, StaleCheck staleCheck, StrategyHandler handler, Runnable exhausted, WikiResponse response)
	{
		if (staleCheck != null && staleCheck.isStale(requestId, target))
		{
			return;
		}
		if (response == null || response.getState() != WikiState.READY || response.getRevision() == null)
		{
			loadStrategy(requestId, target, candidates, index + 1, staleCheck, handler, exhausted);
			return;
		}
		WikiParsingResult parsed = parse(target, response);
		wikiClient.fetchRenderedHtml(response.getRevision().getTitle(), htmlResponse ->
				clientThreadInvoker.accept(() -> handleRenderedStrategyResponse(requestId, target, candidates, index, staleCheck, handler, exhausted, parsed, htmlResponse))
		);
	}

	private void handleRenderedStrategyResponse(int requestId, TargetOption target, List<String> candidates, int index, StaleCheck staleCheck, StrategyHandler handler, Runnable exhausted, WikiParsingResult parsed, WikiResponse htmlResponse)
	{
		if (staleCheck != null && staleCheck.isStale(requestId, target))
		{
			return;
		}
		WikiParsingResult enriched = (htmlResponse != null && htmlResponse.getState() == WikiState.READY && htmlResponse.getRevision() != null)
				? wikiStrategyParser.mergeRenderedHtml(parsed, htmlResponse.getRevision().getText())
				: parsed;
		wikiResultCache.put(candidates.get(index), enriched);
		boolean handled = handler != null && handler.handle(enriched, index == candidates.size() - 1);
		if (!handled)
		{
			loadStrategy(requestId, target, candidates, index + 1, staleCheck, handler, exhausted);
		}
	}

	private void loadVariantPage(int requestId, TargetOption target, List<String> candidates, int index, StaleCheck staleCheck, VariantHandler handler, Runnable exhausted)
	{
		if (index >= candidates.size())
		{
			exhausted.run();
			return;
		}
		String pageTitle = candidates.get(index);
		WikiParsingResult cached = wikiResultCache.get(pageTitle);
		if (cached != null)
		{
			boolean handled = handler != null && handler.handle(cached);
			if (!handled)
			{
				loadVariantPage(requestId, target, candidates, index + 1, staleCheck, handler, exhausted);
			}
			return;
		}
		wikiClient.fetchRevisionText(pageTitle, response ->
				clientThreadInvoker.accept(() -> handleVariantPageResponse(requestId, target, candidates, index, staleCheck, handler, exhausted, response))
		);
	}

	private void handleVariantPageResponse(int requestId, TargetOption target, List<String> candidates, int index, StaleCheck staleCheck, VariantHandler handler, Runnable exhausted, WikiResponse response)
	{
		if (staleCheck != null && staleCheck.isStale(requestId, target))
		{
			return;
		}
		if (response == null || response.getState() != WikiState.READY || response.getRevision() == null)
		{
			loadVariantPage(requestId, target, candidates, index + 1, staleCheck, handler, exhausted);
			return;
		}
		WikiParsingResult parsed = parse(target, response);
		wikiResultCache.put(candidates.get(index), parsed);
		boolean handled = handler != null && handler.handle(parsed);
		if (!handled)
		{
			loadVariantPage(requestId, target, candidates, index + 1, staleCheck, handler, exhausted);
		}
	}

	private void handleStrategyCandidateResponse(int requestId, TargetOption staleTarget, TargetOption parseTarget, String strategyTitle, StaleCheck staleCheck, Consumer<WikiParsingResult> callback, WikiResponse response)
	{
		if (staleCheck != null && staleCheck.isStale(requestId, staleTarget))
		{
			return;
		}
		if (response == null || response.getState() != WikiState.READY || response.getRevision() == null)
		{
			callback.accept(null);
			return;
		}
		WikiParsingResult parsed = parse(parseTarget, response);
		wikiClient.fetchRenderedHtml(response.getRevision().getTitle(), htmlResponse ->
				clientThreadInvoker.accept(() -> handleRenderedStrategyCandidateResponse(requestId, staleTarget, strategyTitle, staleCheck, callback, parsed, htmlResponse))
		);
	}

	private void handleRenderedStrategyCandidateResponse(int requestId, TargetOption target, String strategyTitle, StaleCheck staleCheck, Consumer<WikiParsingResult> callback, WikiParsingResult parsed, WikiResponse htmlResponse)
	{
		if (staleCheck != null && staleCheck.isStale(requestId, target))
		{
			return;
		}
		WikiParsingResult enriched = (htmlResponse != null && htmlResponse.getState() == WikiState.READY && htmlResponse.getRevision() != null)
				? wikiStrategyParser.mergeRenderedHtml(parsed, htmlResponse.getRevision().getText())
				: parsed;
		wikiResultCache.put(strategyTitle, enriched);
		callback.accept(enriched);
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

	private List<String> safeCandidates(List<String> candidates)
	{
		return candidates == null ? Collections.emptyList() : candidates;
	}

	private Runnable safeRunnable(Runnable runnable)
	{
		return runnable != null ? runnable : () -> {};
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