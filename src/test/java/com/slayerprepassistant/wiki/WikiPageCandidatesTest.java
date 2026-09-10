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
		TargetOption target = new TargetOption("Aberrant spectres", "Aberrant spectres", "Strategies/Aberrant spectres");

		assertEquals(
			Arrays.asList("Aberrant spectres/Strategies", "Strategies/Aberrant spectres", "Strategies/Aberrant spectre", "Aberrant spectre/Strategies", "Slayer_task/Aberrant spectres", "Slayer_task/Aberrant spectre"),
			WikiPageCandidates.strategyPages(target, "Aberrant spectres/Strategies"));
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
