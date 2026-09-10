package com.slayerprepassistant.gear;

import com.slayerprepassistant.bank.BankSnapshot;
import com.slayerprepassistant.bank.OwnershipState;
import com.slayerprepassistant.bank.PlayerInventoryState;
import com.slayerprepassistant.items.ItemResolver;
import java.util.Map;
import java.util.Set;

public class GearMatcher
{
	public GearMatcher()
	{
	}

	public OwnershipState ownershipFor(RecommendedItem item, PlayerInventoryState state)
	{
		return ownershipFor(item, state, true, 1);
	}

	public OwnershipState ownershipForInventoryItem(RecommendedItem item, PlayerInventoryState state)
	{
		return ownershipForInventoryItem(item, state, 1);
	}

	public OwnershipState ownershipForInventoryItem(RecommendedItem item, PlayerInventoryState state, int minimumQuantity)
	{
		return ownershipFor(item, state, false, Math.max(1, minimumQuantity));
	}

	private OwnershipState ownershipFor(RecommendedItem item, PlayerInventoryState state, boolean includeEquipment, int minimumQuantity)
	{
		if (item == null || state == null || item.getName() == null || item.getName().trim().isEmpty())
		{
			return OwnershipState.UNKNOWN;
		}

		Set<Integer> itemIds = item.getItemIds();
		String canonicalName = ItemResolver.canonicalKey(item.getName());
		if (canonicalName.isEmpty())
		{
			return OwnershipState.UNKNOWN;
		}

		if (includeEquipment && containsQuantity(itemIds, canonicalName, state.getEquipment(), state::equipmentQuantityForCanonicalName, minimumQuantity))
		{
			return OwnershipState.EQUIPPED;
		}

		if (containsQuantity(itemIds, canonicalName, state.getInventory(), state::inventoryQuantityForCanonicalName, minimumQuantity))
		{
			return OwnershipState.OWNED_IN_INVENTORY;
		}

		BankSnapshot bank = state.getBankSnapshot();
		if (bank == null || !bank.isKnown())
		{
			return OwnershipState.UNKNOWN;
		}

		int bankQuantity = Math.max(bank.quantityForAny(itemIds), bank.quantityForCanonicalName(canonicalName));
		if (bankQuantity >= minimumQuantity)
		{
			return OwnershipState.OWNED_IN_BANK;
		}

		if (itemIds.isEmpty() && hasAmbiguousNumberedVariant(item.getName(), state, includeEquipment))
		{
			return OwnershipState.UNKNOWN;
		}
		return OwnershipState.MISSING;
	}

	private boolean containsQuantity(
		Set<Integer> itemIds,
		String canonicalName,
		Map<Integer, Integer> quantitiesById,
		NameQuantityLookup nameQuantityLookup,
		int minimumQuantity)
	{
		int idQuantity = 0;
		if (itemIds != null && quantitiesById != null)
		{
			for (Integer itemId : itemIds)
			{
				if (itemId != null)
				{
					idQuantity += Math.max(0, quantitiesById.getOrDefault(itemId, 0));
				}
			}
		}
		return Math.max(idQuantity, nameQuantityLookup.quantity(canonicalName)) >= minimumQuantity;
	}

	private boolean hasAmbiguousNumberedVariant(String itemName, PlayerInventoryState state, boolean includeEquipment)
	{
		if (includeEquipment && ItemResolver.hasDifferentNumberedVariant(itemName, state.getEquipmentCanonicalNames()))
		{
			return true;
		}
		return ItemResolver.hasDifferentNumberedVariant(itemName, state.getInventoryCanonicalNames())
			|| ItemResolver.hasDifferentNumberedVariant(itemName, state.getBankSnapshot().getCanonicalItemNames());
	}

	@FunctionalInterface
	private interface NameQuantityLookup
	{
		int quantity(String canonicalName);
	}
}
