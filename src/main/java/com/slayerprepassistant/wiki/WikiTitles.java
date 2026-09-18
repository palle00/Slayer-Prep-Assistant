package com.slayerprepassistant.wiki;

import com.slayerprepassistant.PluginUrls;
import com.slayerprepassistant.items.ItemResolver;
import java.util.Locale;

public final class WikiTitles
{
	private WikiTitles()
	{
	}

	public static String pageUrl(String title)
	{
		if (title == null || title.trim().isEmpty())
		{
			return "";
		}
		return PluginUrls.osrsWikiPageUrl(title);
	}

	public static String singularTitle(String title)
	{
		if (title == null || title.trim().isEmpty())
		{
			return "";
		}
		String trimmed = title.trim();
		return trimmed.length() > 3 && trimmed.endsWith("s") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
	}

	public static String wikiTitle(String input)
	{
		if (input == null || input.trim().isEmpty())
		{
			return "";
		}
		String normalized = ItemResolver.normalize(input);
		if (normalized.isEmpty())
		{
			return input.trim();
		}
		return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
	}

	public static String cacheKey(String title)
	{
		return title == null ? "" : title.trim().toLowerCase(Locale.ROOT);
	}
}
