package com.slayerprepassistant.wiki;

import com.slayerprepassistant.task.TargetOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class WikiSetupLoader
{
	private final WikiClient wikiClient;
	private final WikiStrategyParser wikiStrategyParser;
	private final Consumer<Runnable> clientThreadInvoker;
	private final Map<String, WikiParsingResult> wikiResultCache = new HashMap<>();

	public WikiSetupLoader(WikiClient wikiClient, WikiStrategyParser wikiStrategyParser, Consumer<Runnable> clientThreadInvoker)
	{
		this.wikiClient = wikiClient;
		this.wikiStrategyParser = wikiStrategyParser;
		this.clientThreadInvoker = clientThreadInvoker;
	}

	public void loadStrategy(int requestId, TargetOption target, List<String> candidates, StaleCheck staleCheck, StrategyHandler handler, Runnable exhausted)
	{
		loadStrategy(requestId, target, candidates, 0, staleCheck, handler, exhausted);
	}

	public void loadVariantPage(int requestId, TargetOption target, List<String> candidates, StaleCheck staleCheck, VariantHandler handler, Runnable exhausted)
	{
		loadVariantPage(requestId, target, candidates, 0, staleCheck, handler, exhausted);
	}

	public void loadStrategyCandidate(int requestId, TargetOption staleTarget, TargetOption parseTarget, String strategyTitle, StaleCheck staleCheck, Consumer<WikiParsingResult> callback)
	{
		WikiParsingResult cached = wikiResultCache.get(strategyTitle);
		if (cached != null)
		{
			if (staleCheck.isStale(requestId, staleTarget))
			{
				return;
			}
			callback.accept(cached);
			return;
		}
		wikiClient.fetchRevisionText(strategyTitle, response -> clientThreadInvoker.accept(() -> handleStrategyCandidateResponse(requestId, staleTarget, parseTarget, strategyTitle, staleCheck, callback, response)));
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
			if (!handler.handle(cached, index == candidates.size() - 1))
			{
				loadStrategy(requestId, target, candidates, index + 1, staleCheck, handler, exhausted);
			}
			return;
		}
		wikiClient.fetchRevisionText(strategyTitle, response -> clientThreadInvoker.accept(() -> handleStrategyResponse(requestId, target, candidates, index, staleCheck, handler, exhausted, response)));
	}

	private void handleStrategyResponse(int requestId, TargetOption target, List<String> candidates, int index, StaleCheck staleCheck, StrategyHandler handler, Runnable exhausted, WikiResponse response)
	{
		if (staleCheck.isStale(requestId, target))
		{
			return;
		}
		if (response.getState() != WikiState.READY || response.getRevision() == null)
		{
			loadStrategy(requestId, target, candidates, index + 1, staleCheck, handler, exhausted);
			return;
		}
		WikiParsingResult parsed = parse(target, response);
		wikiClient.fetchRenderedHtml(response.getRevision().getTitle(), htmlResponse ->
			clientThreadInvoker.accept(() -> handleRenderedStrategyResponse(requestId, target, candidates, index, staleCheck, handler, exhausted, parsed, htmlResponse)));
	}

	private void handleRenderedStrategyResponse(int requestId, TargetOption target, List<String> candidates, int index, StaleCheck staleCheck, StrategyHandler handler, Runnable exhausted, WikiParsingResult parsed, WikiResponse htmlResponse)
	{
		if (staleCheck.isStale(requestId, target))
		{
			return;
		}
		WikiParsingResult enriched = htmlResponse.getState() == WikiState.READY && htmlResponse.getRevision() != null
			? wikiStrategyParser.mergeRenderedHtml(parsed, htmlResponse.getRevision().getText())
			: parsed;
		wikiResultCache.put(candidates.get(index), enriched);
		if (!handler.handle(enriched, index == candidates.size() - 1))
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
			if (!handler.handle(cached))
			{
				loadVariantPage(requestId, target, candidates, index + 1, staleCheck, handler, exhausted);
			}
			return;
		}
		wikiClient.fetchRevisionText(pageTitle, response -> clientThreadInvoker.accept(() -> handleVariantPageResponse(requestId, target, candidates, index, staleCheck, handler, exhausted, response)));
	}

	private void handleVariantPageResponse(int requestId, TargetOption target, List<String> candidates, int index, StaleCheck staleCheck, VariantHandler handler, Runnable exhausted, WikiResponse response)
	{
		if (staleCheck.isStale(requestId, target))
		{
			return;
		}
		if (response.getState() != WikiState.READY || response.getRevision() == null)
		{
			loadVariantPage(requestId, target, candidates, index + 1, staleCheck, handler, exhausted);
			return;
		}
		WikiParsingResult parsed = parse(target, response);
		wikiResultCache.put(candidates.get(index), parsed);
		if (!handler.handle(parsed))
		{
			loadVariantPage(requestId, target, candidates, index + 1, staleCheck, handler, exhausted);
		}
	}

	private void handleStrategyCandidateResponse(int requestId, TargetOption staleTarget, TargetOption parseTarget, String strategyTitle, StaleCheck staleCheck, Consumer<WikiParsingResult> callback, WikiResponse response)
	{
		if (staleCheck.isStale(requestId, staleTarget))
		{
			return;
		}
		if (response.getState() != WikiState.READY || response.getRevision() == null)
		{
			callback.accept(null);
			return;
		}
		WikiParsingResult parsed = parse(parseTarget, response);
		wikiClient.fetchRenderedHtml(response.getRevision().getTitle(), htmlResponse ->
			clientThreadInvoker.accept(() -> handleRenderedStrategyCandidateResponse(requestId, staleTarget, strategyTitle, staleCheck, callback, parsed, htmlResponse)));
	}

	private void handleRenderedStrategyCandidateResponse(int requestId, TargetOption target, String strategyTitle, StaleCheck staleCheck, Consumer<WikiParsingResult> callback, WikiParsingResult parsed, WikiResponse htmlResponse)
	{
		if (staleCheck.isStale(requestId, target))
		{
			return;
		}
		WikiParsingResult enriched = htmlResponse.getState() == WikiState.READY && htmlResponse.getRevision() != null
			? wikiStrategyParser.mergeRenderedHtml(parsed, htmlResponse.getRevision().getText())
			: parsed;
		wikiResultCache.put(strategyTitle, enriched);
		callback.accept(enriched);
	}

	private WikiParsingResult parse(TargetOption target, WikiResponse response)
	{
		return wikiStrategyParser.parse(
			target.getDisplayName(),
			response.getRevision().getTitle(),
			WikiTitles.pageUrl(response.getRevision().getTitle()),
			response.getRevision().getRevisionId(),
			response.getRevision().getText());
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
