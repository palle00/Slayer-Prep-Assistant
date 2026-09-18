package com.slayerprepassistant;

import java.net.URI;
import java.net.URISyntaxException;
import okhttp3.HttpUrl;

public final class PluginUrls
{
	private static final String OSRS_WIKI_SCHEME = "https";

	public static final String OSRS_WIKI_HOST = "oldschool.runescape.wiki";
	public static final String OSRS_WIKI_BASE_URL = "https://oldschool.runescape.wiki";
	public static final String OSRS_WIKI_API_URL = "https://oldschool.runescape.wiki/api.php";
	public static final String OSRS_WIKI_PAGE_PATH_PREFIX = "/w/";
	public static final String OSRS_WIKI_IMAGE_PATH_PREFIX = "/images/";

	private PluginUrls()
	{
	}

	public static HttpUrl.Builder osrsWikiApiEndpointBuilder()
	{
		return parseDeclaredUrlOrThrow(OSRS_WIKI_API_URL).newBuilder();
	}

	public static String osrsWikiPageUrl(String wikiTitle)
	{
		if (wikiTitle == null || wikiTitle.trim().isEmpty())
		{
			return "";
		}
		try
		{
			String wikiPagePath = OSRS_WIKI_PAGE_PATH_PREFIX + wikiTitle.trim().replace(' ', '_');
			return new URI(OSRS_WIKI_SCHEME, OSRS_WIKI_HOST, wikiPagePath, null)
				.toASCIIString();
		}
		catch (URISyntaxException ex)
		{
			return "";
		}
	}

	public static HttpUrl osrsWikiImageUrlFromMediaWikiSource(String mediaWikiThumbnailSource)
	{
		HttpUrl wikiOwnedSource = parseOnlyIfSourceStillPointsToOsrsWiki(mediaWikiThumbnailSource);
		if (!isOsrsWikiImagePath(wikiOwnedSource))
		{
			return null;
		}

		return rebuildImageUrlFromDeclaredWikiBase(wikiOwnedSource);
	}

	private static boolean isOsrsWikiImagePath(HttpUrl sourceUrl)
	{
		return sourceUrl != null && sourceUrl.encodedPath().startsWith(OSRS_WIKI_IMAGE_PATH_PREFIX);
	}

	private static HttpUrl rebuildImageUrlFromDeclaredWikiBase(HttpUrl wikiOwnedSource)
	{
		HttpUrl.Builder builder = parseDeclaredUrlOrThrow(OSRS_WIKI_BASE_URL)
			.newBuilder()
			.encodedPath(wikiOwnedSource.encodedPath());
		if (wikiOwnedSource.encodedQuery() != null)
		{
			builder.encodedQuery(wikiOwnedSource.encodedQuery());
		}
		return builder.build();
	}

	private static HttpUrl parseOnlyIfSourceStillPointsToOsrsWiki(String mediaWikiThumbnailSource)
	{
		if (mediaWikiThumbnailSource == null || mediaWikiThumbnailSource.trim().isEmpty())
		{
			return null;
		}

		String normalizedSource = mediaWikiThumbnailSource.trim();
		if (normalizedSource.startsWith(OSRS_WIKI_IMAGE_PATH_PREFIX))
		{
			return parseDeclaredUrlOrThrow(OSRS_WIKI_BASE_URL).resolve(normalizedSource);
		}
		if (normalizedSource.startsWith("//"))
		{
			normalizedSource = OSRS_WIKI_SCHEME + ":" + normalizedSource;
		}

		HttpUrl parsedSource = HttpUrl.parse(normalizedSource);
		if (parsedSource == null || !OSRS_WIKI_HOST.equalsIgnoreCase(parsedSource.host()))
		{
			return null;
		}
		return parsedSource;
	}

	private static HttpUrl parseDeclaredUrlOrThrow(String declaredUrl)
	{
		HttpUrl parsed = HttpUrl.parse(declaredUrl);
		if (parsed == null)
		{
			throw new IllegalStateException("Invalid plugin URL: " + declaredUrl);
		}
		return parsed;
	}
}
