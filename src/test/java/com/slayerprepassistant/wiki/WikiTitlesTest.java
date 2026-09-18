package com.slayerprepassistant.wiki;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WikiTitlesTest
{
	@Test
	public void pageUrlBuildsBrowserSafeWikiUrl()
	{
		assertEquals(
			"https://oldschool.runescape.wiki/w/Bank_filler",
			WikiTitles.pageUrl("Bank filler"));
	}

	@Test
	public void singularTitleRemovesSimplePluralSuffix()
	{
		assertEquals("Bank filler", WikiTitles.singularTitle("Bank fillers"));
	}

	@Test
	public void wikiTitleNormalizesMonsterNames()
	{
		assertEquals("Bank fillers", WikiTitles.wikiTitle("Bank Fillers"));
		assertEquals("Bank filler", WikiTitles.wikiTitle("Bank filler"));
	}

	@Test
	public void cacheKeyNormalizesCaseAndWhitespace()
	{
		assertEquals("bank fillers", WikiTitles.cacheKey("  Bank Fillers  "));
	}
}
