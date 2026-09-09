package com.slayerprepassistant.bank;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;

public class BankSnapshotService
{
	private BankSnapshot snapshot = BankSnapshot.emptyUnknown();

	public void updateFromBankContainer(ItemContainer container)
	{
		updateFromBankContainer(container, null);
	}

	public void updateFromBankContainer(ItemContainer container, Client client)
	{
		if (container == null)
		{
			return;
		}
		snapshot = new BankSnapshot(toQuantities(container), toNames(container, client), Instant.now());
	}

	public BankSnapshot getSnapshot()
	{
		return snapshot;
	}

	public static Map<Integer, Integer> toQuantities(ItemContainer container)
	{
		Map<Integer, Integer> quantities = new HashMap<>();
		if (container == null)
		{
			return quantities;
		}
		for (Item item : container.getItems())
		{
			if (item.getId() > 0 && item.getQuantity() > 0)
			{
				quantities.merge(item.getId(), item.getQuantity(), Integer::sum);
			}
		}
		return quantities;
	}

	public static Set<String> toNames(ItemContainer container, Client client)
	{
		Set<String> names = new HashSet<>();
		if (container == null || client == null)
		{
			return names;
		}
		for (Item item : container.getItems())
		{
			if (item.getId() <= 0 || item.getQuantity() <= 0)
			{
				continue;
			}
			ItemComposition composition = client.getItemDefinition(item.getId());
			if (composition != null && composition.getName() != null && !composition.getName().trim().isEmpty())
			{
				names.add(composition.getName());
			}
		}
		return names;
	}
}
