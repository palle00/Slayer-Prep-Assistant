package com.slayerprepassistant.gear;

import com.slayerprepassistant.bank.OwnershipState;

public class GearMatch
{
	private final GearSlot slot;
	private final RecommendedItem item;
	private final OwnershipState ownershipState;
	private final int tierPriority;

	public GearMatch(GearSlot slot, RecommendedItem item, OwnershipState ownershipState, int tierPriority)
	{
		this.slot = slot;
		this.item = item;
		this.ownershipState = ownershipState;
		this.tierPriority = tierPriority;
	}

	public GearSlot getSlot()
	{
		return slot;
	}

	public RecommendedItem getItem()
	{
		return item;
	}

	public OwnershipState getOwnershipState()
	{
		return ownershipState;
	}

	public int getTierPriority()
	{
		return tierPriority;
	}
}
