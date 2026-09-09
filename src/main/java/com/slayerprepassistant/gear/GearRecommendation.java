package com.slayerprepassistant.gear;

import java.util.Collections;
import java.util.List;

public class GearRecommendation
{
	private final GearSlot slot;
	private final List<GearTier> tiers;
	private final boolean optional;

	public GearRecommendation(GearSlot slot, List<GearTier> tiers)
	{
		this(slot, tiers, false);
	}

	public GearRecommendation(GearSlot slot, List<GearTier> tiers, boolean optional)
	{
		this.slot = slot;
		this.tiers = Collections.unmodifiableList(tiers);
		this.optional = optional;
	}

	public GearSlot getSlot()
	{
		return slot;
	}

	public List<GearTier> getTiers()
	{
		return tiers;
	}

	public boolean isOptional()
	{
		return optional;
	}
}
