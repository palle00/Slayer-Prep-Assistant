package com.slayerprepassistant.bank;

import java.time.Instant;
import java.util.Collections;
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
	private volatile BankSnapshot snapshot = BankSnapshot.emptyUnknown();

	public void updateFromBankContainer(ItemContainer container, Client client)
	{
		if (container == null)
		{
			return;
		}

		Map<Integer, ItemComposition> compositionsById = toCompositions(container, client);
		Set<Integer> placeholderIds = placeholderIds(compositionsById);
		this.snapshot = new BankSnapshot(toQuantities(container, placeholderIds), toNames(container, compositionsById, placeholderIds), Instant.now());
	}

	public BankSnapshot getSnapshot()
	{
		return snapshot;
	}

	public static Map<Integer, Integer> toQuantities(ItemContainer container)
	{
		return toQuantities(container, Collections.emptySet());
	}

	static Map<Integer, Integer> toQuantities(ItemContainer container, Set<Integer> ignoredIds)
	{
		if (container == null)
		{
			return Collections.emptyMap();
		}
		Map<Integer, Integer> quantities = new HashMap<>();
		for (Item item : container.getItems())
		{
			if (item != null && item.getId() > 0 && item.getQuantity() > 0 && (ignoredIds == null || !ignoredIds.contains(item.getId())))
			{
				quantities.merge(item.getId(), item.getQuantity(), Integer::sum);
			}
		}
		return quantities;
	}

	public static Set<String> toNames(ItemContainer container, Client client)
	{
		if (container == null || client == null)
		{
			return Collections.emptySet();
		}
		return toNames(container, toCompositions(container, client), Collections.emptySet());
	}

	private static Set<String> toNames(ItemContainer container, Map<Integer, ItemComposition> compositionsById, Set<Integer> ignoredIds)
	{
		Set<String> names = new HashSet<>();
		for (Item item : container.getItems())
		{
			if (item == null || item.getId() <= 0 || item.getQuantity() <= 0 || (ignoredIds != null && ignoredIds.contains(item.getId())))
			{
				continue;
			}
			ItemComposition composition = compositionsById.get(item.getId());
			if (composition != null)
			{
				String name = composition.getName();
				if (name != null && !name.trim().isEmpty())
				{
					names.add(name);
				}
			}
		}
		return names;
	}

	private static Map<Integer, ItemComposition> toCompositions(ItemContainer container, Client client)
	{
		if (container == null || client == null)
		{
			return Collections.emptyMap();
		}

		Map<Integer, ItemComposition> compositionsById = new HashMap<>();
		for (Item item : container.getItems())
		{
			if (item != null && item.getId() > 0 && item.getQuantity() > 0 && !compositionsById.containsKey(item.getId()))
			{
				compositionsById.put(item.getId(), client.getItemDefinition(item.getId()));
			}
		}
		return compositionsById;
	}

	private static Set<Integer> placeholderIds(Map<Integer, ItemComposition> compositionsById)
	{
		if (compositionsById == null || compositionsById.isEmpty())
		{
			return Collections.emptySet();
		}

		Set<Integer> placeholderIds = new HashSet<>();
		for (Map.Entry<Integer, ItemComposition> entry : compositionsById.entrySet())
		{
			ItemComposition composition = entry.getValue();
			if (composition != null && composition.getPlaceholderTemplateId() >= 0)
			{
				placeholderIds.add(entry.getKey());
			}
		}
		return placeholderIds;
	}
}
