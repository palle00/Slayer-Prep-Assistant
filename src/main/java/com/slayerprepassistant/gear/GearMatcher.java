package com.slayerprepassistant.gear;

import com.slayerprepassistant.bank.OwnershipState;
import com.slayerprepassistant.bank.PlayerInventoryState;
import com.slayerprepassistant.items.ItemResolver;

import java.util.Collections;
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
		return ownershipFor(item, state, true);
	}

	public OwnershipState ownershipForInventoryItem(RecommendedItem item, PlayerInventoryState state)
	{
		return ownershipFor(item, state, false);
	}

	private OwnershipState ownershipFor(RecommendedItem item, PlayerInventoryState state, boolean includeEquipment)
	{
		if (item == null || state == null)
		{
			return OwnershipState.UNKNOWN;
		}

		Set<Integer> itemIds = item.getItemIds();

		if (includeEquipment && containsItem(itemIds, item.getName(), state.getEquipment().keySet(), state.getEquipmentNames()))
		{
			return OwnershipState.EQUIPPED;
		}

		if (containsItem(itemIds, item.getName(), state.getInventory().keySet(), state.getInventoryNames()))
		{
			return OwnershipState.OWNED_IN_INVENTORY;
		}

		if (!state.getBankSnapshot().isKnown())
		{
			return OwnershipState.UNKNOWN;
		}

		boolean bankIdMatch = !itemIds.isEmpty() && state.getBankSnapshot().containsAny(itemIds);
		boolean bankNameMatch = itemResolver.matches(item.getName(), state.getBankSnapshot().getItemNames());
		if (bankIdMatch || bankNameMatch)
		{
			return OwnershipState.OWNED_IN_BANK;
		}

		return itemIds.isEmpty() ? OwnershipState.UNKNOWN : OwnershipState.MISSING;
	}

	private boolean containsItem(Set<Integer> targetIds, String itemName, Set<Integer> containerIds, Set<String> containerNames)
	{
		if (!targetIds.isEmpty() && !Collections.disjoint(containerIds, targetIds))
		{
			return true;
		}
		return itemResolver.matches(itemName, containerNames);
	}
}