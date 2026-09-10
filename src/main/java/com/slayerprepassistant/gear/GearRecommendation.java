package com.slayerprepassistant.gear;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GearRecommendation
{
	private final GearSlot slot;
	private final List<GearTier> tiers;

	public GearRecommendation(GearSlot slot, List<GearTier> tiers)
	{
		this.slot = slot;
		this.tiers = tiers == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(tiers));
	}

	public GearSlot getSlot()
	{
		return slot;
	}

	public List<GearTier> getTiers()
	{
		return tiers;
	}

}