package com.slayerprepassistant.task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.List;
import org.junit.Test;

public class TaskTargetResolverTest
{
	private final TaskTargetResolver resolver = new TaskTargetResolver(new TaskResolutionOverrides());

	@Test
	public void taskResolvesToSingleWikiCandidateUntilVariantsAreParsed()
	{
		List<TargetOption> targets = resolver.resolve(new SlayerTaskContext("black dragons", 45, 45, "", true));

		assertEquals(1, targets.size());
		assertEquals("Black dragons", targets.get(0).getDisplayName());
		assertEquals("Black dragons", targets.get(0).getWikiPage());
	}

	@Test
	public void unknownTaskFallsBackToManualWikiCandidate()
	{
		List<TargetOption> targets = resolver.resolve(new SlayerTaskContext("strange creature", 20, 20, "", true));

		assertEquals(1, targets.size());
		assertEquals("Strange creature", targets.get(0).getDisplayName());
		assertFalse(targets.get(0).getStrategyPage().isEmpty());
	}

	@Test
	public void multiWordTaskKeepsWikiTitleCase()
	{
		List<TargetOption> targets = resolver.resolve(new SlayerTaskContext("Aberrant Spectres", 106, 106, "", true));

		assertEquals(1, targets.size());
		assertEquals("Aberrant spectres", targets.get(0).getWikiPage());
	}
}
