package com.slayerprepassistant.gear;

import java.util.Collections;
import java.util.List;

public class GearTier
{
	private final int priority;
	private final List<RecommendedItem> alternatives;

	public GearTier(int priority, List<RecommendedItem> alternatives)
	{
		this.priority = priority;
		this.alternatives = Collections.unmodifiableList(alternatives);
	}

	public int getPriority()
	{
		return priority;
	}

	public List<RecommendedItem> getAlternatives()
	{
		return alternatives;
	}
}
