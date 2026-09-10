package com.slayerprepassistant.gear;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class RecommendedItem
{
	private final String name;
	private final Set<Integer> itemIds;

	public RecommendedItem(String name)
	{
		this(name, Collections.emptySet());
	}

	public RecommendedItem(String name, Set<Integer> itemIds)
	{
		this.name = name == null ? "" : name;
		this.itemIds = itemIds == null || itemIds.isEmpty()
			? Collections.emptySet()
			: Collections.unmodifiableSet(new HashSet<>(itemIds));
	}

	public String getName()
	{
		return name;
	}

	public Set<Integer> getItemIds()
	{
		return itemIds;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
