package com.slayerprepassistant.task;

import com.slayerprepassistant.wiki.WikiTitles;
import java.util.ArrayList;
import java.util.List;

public class TaskTargetResolver
{
	public List<TargetOption> resolve(SlayerTaskContext taskContext)
	{
		if (taskContext == null || !taskContext.isActive())
		{
			return new ArrayList<>();
		}
		String title = WikiTitles.wikiTitle(taskContext.getTaskName());
		return java.util.Collections.singletonList(new TargetOption(title, title, "Strategies/" + title));
	}
}
