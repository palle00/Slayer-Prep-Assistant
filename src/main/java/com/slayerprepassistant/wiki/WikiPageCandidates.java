package com.slayerprepassistant.wiki;

import com.slayerprepassistant.task.TargetOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class WikiPageCandidates
{
	private WikiPageCandidates()
	{
	}

	public static List<String> strategyPages(TargetOption target, String preferredStrategyPage)
	{
		Set<String> candidates = new LinkedHashSet<>();
		add(candidates, preferredStrategyPage);

		if (target == null)
		{
			return new ArrayList<>(candidates);
		}

		String wikiPage = target.getWikiPage();
		if (wikiPage != null && !wikiPage.trim().isEmpty())
		{
			String singular = WikiTitles.singularTitle(wikiPage);
			add(candidates, target.getStrategyPage());
			add(candidates, "Strategies/" + wikiPage);
			add(candidates, wikiPage + "/Strategies");
			add(candidates, "Strategies/" + singular);
			add(candidates, singular + "/Strategies");
		}

		return new ArrayList<>(candidates);
	}

	public static List<String> variantPages(TargetOption target)
	{
		Set<String> candidates = new LinkedHashSet<>();
		if (target != null)
		{
			String wikiPage = target.getWikiPage();
			addVariantPages(candidates, wikiPage);
			if (wikiPage != null && !wikiPage.trim().isEmpty())
			{
				addVariantPages(candidates, WikiTitles.singularTitle(wikiPage));
			}
		}
		return new ArrayList<>(candidates);
	}

	private static void addVariantPages(Set<String> candidates, String pageTitle)
	{
		if (pageTitle == null || pageTitle.trim().isEmpty())
		{
			return;
		}
		String cleaned = pageTitle.trim();
		candidates.add(cleaned);
		if (!cleaned.startsWith("Slayer_task/"))
		{
			candidates.add("Slayer_task/" + cleaned);
		}
	}

	private static void add(Set<String> candidates, String pageTitle)
	{
		if (pageTitle != null && !pageTitle.trim().isEmpty())
		{
			candidates.add(pageTitle.trim());
		}
	}
}