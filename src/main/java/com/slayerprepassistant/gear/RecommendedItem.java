package com.slayerprepassistant.gear;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class RecommendedItem
{
	private final String name;
	private final Set<Integer> itemIds;
	private final String notes;

	public RecommendedItem(String name)
	{
		this(name, Collections.emptySet(), "");
	}

	public RecommendedItem(String name, Set<Integer> itemIds, String notes)
	{
		this.name = name;
		this.itemIds = Collections.unmodifiableSet(new HashSet<>(itemIds));
		this.notes = notes == null ? "" : notes;
	}

	public String getName()
	{
		return name;
	}

	public Set<Integer> getItemIds()
	{
		return itemIds;
	}

	public String getNotes()
	{
		return notes;
	}

	public String normalizedName()
	{
		return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
	}

	@Override
	public String toString()
	{
		return name;
	}
}
