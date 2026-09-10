package com.slayerprepassistant.task;

import com.slayerprepassistant.wiki.WikiTitles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TaskTargetResolver
{
	public List<TargetOption> resolve(SlayerTaskContext taskContext)
	{
		if (taskContext == null || !taskContext.isActive())
		{
			return Collections.emptyList();
		}

		String taskName = taskContext.getTaskName();
		if (taskName == null || taskName.trim().isEmpty())
		{
			return Collections.emptyList();
		}

		String title = WikiTitles.wikiTitle(taskName);
		if (title == null || title.trim().isEmpty())
		{
			title = taskName.trim();
		}

		TargetOption option = new TargetOption(title, title, "Strategies/" + title);
		return Collections.singletonList(option);
	}
}