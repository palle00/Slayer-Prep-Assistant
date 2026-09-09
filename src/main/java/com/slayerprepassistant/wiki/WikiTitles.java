package com.slayerprepassistant.wiki;

import com.slayerprepassistant.items.ItemResolver;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public final class WikiTitles
{
	private static final String HOST = "oldschool.runescape.wiki";

	private WikiTitles()
	{
	}

	public static String pageUrl(String title)
	{
		if (title == null || title.trim().isEmpty())
		{
			return "";
		}
		try
		{
			return new URI("https", HOST, "/w/" + title.trim().replace(' ', '_'), null).toASCIIString();
		}
		catch (URISyntaxException ex)
		{
			return "";
		}
	}

	public static String singularTitle(String title)
	{
		if (title == null)
		{
			return "";
		}
		String trimmed = title.trim();
		return trimmed.length() > 3 && trimmed.endsWith("s") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
	}

	public static String wikiTitle(String input)
	{
		String normalized = ItemResolver.normalize(input);
		if (normalized.isEmpty())
		{
			return input == null ? "" : input.trim();
		}
		return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
	}

	public static String cacheKey(String title)
	{
		return title == null ? "" : title.trim().toLowerCase(Locale.ROOT);
	}
}
