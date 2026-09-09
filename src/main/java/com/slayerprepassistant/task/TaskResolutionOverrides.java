package com.slayerprepassistant.task;

import com.slayerprepassistant.items.ItemResolver;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskResolutionOverrides
{
	private final Map<String, List<TargetOption>> overrides = new HashMap<>();

	public TaskResolutionOverrides()
	{
	}

	public List<TargetOption> resolve(String taskName)
	{
		return overrides.getOrDefault(ItemResolver.normalize(taskName), Collections.emptyList());
	}

}
