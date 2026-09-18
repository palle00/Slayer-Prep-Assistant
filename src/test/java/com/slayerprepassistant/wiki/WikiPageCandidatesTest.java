package com.slayerprepassistant.wiki;

import static org.junit.Assert.assertEquals;

import com.slayerprepassistant.task.TargetOption;
import java.util.Arrays;
import org.junit.Test;

public class WikiPageCandidatesTest
{
	@Test
	public void strategyPagesKeepPreferredFirstAndRemoveDuplicates()
	{
		TargetOption target = new TargetOption("Bank fillers", "Bank fillers", "Strategies/Bank fillers");

		assertEquals(
			Arrays.asList("Bank fillers/Strategies", "Strategies/Bank fillers", "Strategies/Bank filler", "Bank filler/Strategies", "Slayer_task/Bank fillers", "Slayer_task/Bank filler"),
			WikiPageCandidates.strategyPages(target, "Bank fillers/Strategies"));
	}

	@Test
	public void variantPagesTryTaskPageAliases()
	{
		TargetOption target = new TargetOption("Dust devils", "Dust devils", "Strategies/Dust devils");

		assertEquals(
			Arrays.asList("Dust devils", "Slayer_task/Dust devils", "Dust devil", "Slayer_task/Dust devil"),
			WikiPageCandidates.variantPages(target));
	}
}
