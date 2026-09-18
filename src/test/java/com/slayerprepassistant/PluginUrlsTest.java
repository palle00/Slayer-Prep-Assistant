package com.slayerprepassistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class PluginUrlsTest
{
	@Test
	public void wikiApiUrlUsesDeclaredEndpoint()
	{
		assertEquals(
			"https://oldschool.runescape.wiki/api.php",
			PluginUrls.osrsWikiApiEndpointBuilder().build().toString());
	}

	@Test
	public void wikiPageUrlUsesDeclaredWikiHost()
	{
		assertEquals(
			"https://oldschool.runescape.wiki/w/Bank_filler",
			PluginUrls.osrsWikiPageUrl("Bank filler"));
	}

	@Test
	public void wikiImageUrlRebuildsWikiImagePathAgainstDeclaredHost()
	{
		assertEquals(
			"https://oldschool.runescape.wiki/images/Item.png?abc=123",
			PluginUrls.osrsWikiImageUrlFromMediaWikiSource("https://oldschool.runescape.wiki/images/Item.png?abc=123").toString());
	}

	@Test
	public void wikiImageUrlAcceptsRelativeWikiImagePath()
	{
		assertEquals(
			"https://oldschool.runescape.wiki/images/Item.png?abc=123",
			PluginUrls.osrsWikiImageUrlFromMediaWikiSource("/images/Item.png?abc=123").toString());
	}

	@Test
	public void wikiImageUrlRejectsForeignHost()
	{
		assertNull(PluginUrls.osrsWikiImageUrlFromMediaWikiSource("https://example.com/images/Item.png"));
	}

	@Test
	public void wikiImageUrlRejectsNonImageWikiPath()
	{
		assertNull(PluginUrls.osrsWikiImageUrlFromMediaWikiSource("https://oldschool.runescape.wiki/w/Bank_filler"));
	}
}
