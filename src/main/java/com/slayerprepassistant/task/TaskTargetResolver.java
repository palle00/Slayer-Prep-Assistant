package com.slayerprepassistant.task;

import com.slayerprepassistant.wiki.WikiTitles;
import java.util.ArrayList;
import java.util.List;

public class TaskTargetResolver
{
	private final TaskResolutionOverrides overrides;

	public TaskTargetResolver(TaskResolutionOverrides overrides)
	{
		this.overrides = overrides;
	}

	public List<TargetOption> resolve(SlayerTaskContext taskContext)
	{
		if (taskContext == null || !taskContext.isActive())
		{
			return new ArrayList<>();
		}
		List<TargetOption> resolved = overrides.resolve(taskContext.getTaskName());
		if (!resolved.isEmpty())
		{
			return resolved;
		}
		String title = WikiTitles.wikiTitle(taskContext.getTaskName());
		return java.util.Collections.singletonList(new TargetOption(title, title, title, "Strategies/" + title, null, null, "Generic Wiki-derived candidate pending resolution", true));
	}
}
