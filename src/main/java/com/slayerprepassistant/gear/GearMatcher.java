package com.slayerprepassistant.gear;

import com.slayerprepassistant.bank.OwnershipState;
import com.slayerprepassistant.bank.PlayerInventoryState;
import com.slayerprepassistant.items.ItemResolver;
import java.util.Set;

public class GearMatcher
{
	private final ItemResolver itemResolver;

	public GearMatcher()
	{
		this(new ItemResolver());
	}

	public GearMatcher(ItemResolver itemResolver)
	{
		this.itemResolver = itemResolver;
	}

	public OwnershipState ownershipFor(RecommendedItem item, PlayerInventoryState state)
	{
		Set<Integer> itemIds = item.getItemIds();
		if (!itemIds.isEmpty() && containsAny(state.getEquipment().keySet(), itemIds))
		{
			return OwnershipState.EQUIPPED;
		}
		if (itemResolver.matches(item.getName(), state.getEquipmentNames()))
		{
			return OwnershipState.EQUIPPED;
		}
		if (!itemIds.isEmpty() && containsAny(state.getInventory().keySet(), itemIds))
		{
			return OwnershipState.OWNED_IN_INVENTORY;
		}
		if (itemResolver.matches(item.getName(), state.getInventoryNames()))
		{
			return OwnershipState.OWNED_IN_INVENTORY;
		}
		if (!state.getBankSnapshot().isKnown())
		{
			return OwnershipState.UNKNOWN;
		}
		if (!itemIds.isEmpty() && state.getBankSnapshot().containsAny(itemIds))
		{
			return OwnershipState.OWNED_IN_BANK;
		}
		if (itemResolver.matches(item.getName(), state.getBankSnapshot().getItemNames()))
		{
			return OwnershipState.OWNED_IN_BANK;
		}
		if (itemIds.isEmpty())
		{
			return OwnershipState.UNKNOWN;
		}
		return OwnershipState.MISSING;
	}

	private boolean containsAny(Set<Integer> ownedIds, Set<Integer> candidateIds)
	{
		for (Integer candidateId : candidateIds)
		{
			if (ownedIds.contains(candidateId))
			{
				return true;
			}
		}
		return false;
	}
}
