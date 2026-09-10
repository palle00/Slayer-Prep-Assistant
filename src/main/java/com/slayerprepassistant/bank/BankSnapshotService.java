package com.slayerprepassistant.bank;

import com.slayerprepassistant.items.ItemResolver;
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
		ContainerSnapshot captured = capture(container, client, true);
		BankSnapshot next = new BankSnapshot(captured.quantitiesById, captured.itemNames, captured.quantitiesByCanonicalName, Instant.now());
		if (!next.equals(snapshot))
		{
			snapshot = next;
		}
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
			if (isUsable(item) && (ignoredIds == null || !ignoredIds.contains(item.getId())))
			{
				quantities.merge(item.getId(), item.getQuantity(), Integer::sum);
			}
		}
		return quantities;
	}

	public static Set<String> toNames(ItemContainer container, Client client)
	{
		return capture(container, client, false).itemNames;
	}

	static ContainerSnapshot capture(ItemContainer container, Client client, boolean ignorePlaceholders)
	{
		if (container == null)
		{
			return ContainerSnapshot.empty();
		}

		Map<Integer, Integer> quantities = new HashMap<>();
		Set<String> names = new HashSet<>();
		Map<String, Integer> quantitiesByCanonicalName = new HashMap<>();
		Map<Integer, ItemComposition> compositionsById = new HashMap<>();

		for (Item item : container.getItems())
		{
			if (!isUsable(item))
			{
				continue;
			}

			ItemComposition composition = null;
			if (client != null)
			{
				composition = compositionsById.computeIfAbsent(item.getId(), client::getItemDefinition);
				if (ignorePlaceholders && composition != null && composition.getPlaceholderTemplateId() >= 0)
				{
					continue;
				}
			}

			quantities.merge(item.getId(), item.getQuantity(), Integer::sum);
			if (composition == null || composition.getName() == null || composition.getName().trim().isEmpty())
			{
				continue;
			}

			String name = composition.getName();
			names.add(name);
			String canonical = ItemResolver.canonicalKey(name);
			if (!canonical.isEmpty())
			{
				quantitiesByCanonicalName.merge(canonical, item.getQuantity(), Integer::sum);
			}
		}

		return new ContainerSnapshot(quantities, names, quantitiesByCanonicalName);
	}

	private static boolean isUsable(Item item)
	{
		return item != null && item.getId() > 0 && item.getQuantity() > 0;
	}

	static final class ContainerSnapshot
	{
		final Map<Integer, Integer> quantitiesById;
		final Set<String> itemNames;
		final Map<String, Integer> quantitiesByCanonicalName;

		private ContainerSnapshot(Map<Integer, Integer> quantitiesById, Set<String> itemNames, Map<String, Integer> quantitiesByCanonicalName)
		{
			this.quantitiesById = quantitiesById;
			this.itemNames = itemNames;
			this.quantitiesByCanonicalName = quantitiesByCanonicalName;
		}

		private static ContainerSnapshot empty()
		{
			return new ContainerSnapshot(Collections.emptyMap(), Collections.emptySet(), Collections.emptyMap());
		}
	}
}
