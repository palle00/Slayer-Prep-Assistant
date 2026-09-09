package com.slayerprepassistant.wiki;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WikiTitlesTest
{
	@Test
	public void pageUrlBuildsBrowserSafeWikiUrl()
	{
		assertEquals(
			"https://oldschool.runescape.wiki/w/Aberrant_spectres",
			WikiTitles.pageUrl("Aberrant spectres"));
	}

	@Test
	public void singularTitleRemovesSimplePluralSuffix()
	{
		assertEquals("Aberrant spectre", WikiTitles.singularTitle("Aberrant spectres"));
	}

	@Test
	public void wikiTitleNormalizesMonsterNames()
	{
		assertEquals("Aberrant spectres", WikiTitles.wikiTitle("Aberrant Spectres"));
		assertEquals("Greater abyssal demon", WikiTitles.wikiTitle("Greater abyssal demon"));
	}

	@Test
	public void cacheKeyNormalizesCaseAndWhitespace()
	{
		assertEquals("abyssal demons", WikiTitles.cacheKey("  Abyssal Demons  "));
	}
}
