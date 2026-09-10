package com.slayerprepassistant.items;

import com.slayerprepassistant.gear.RecommendedItem;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.client.game.ItemManager;
import net.runelite.http.api.item.ItemPrice;

public class RuneLiteItemLookup
{
	private final ItemManager itemManager;
	private final Map<String, Integer> itemIdCache = new HashMap<>();
	private final Set<String> itemIdMisses = new HashSet<>();

	public RuneLiteItemLookup(ItemManager itemManager)
	{
		this.itemManager = itemManager;
	}

	public synchronized Integer itemId(RecommendedItem item)
	{
		if (item == null || item.getName() == null || item.getName().trim().isEmpty())
		{
			return null;
		}

		String normalized = ItemResolver.normalize(item.getName());
		if (normalized.isEmpty())
		{
			return null;
		}

		Integer cached = itemIdCache.get(normalized);
		if (cached != null)
		{
			return cached;
		}
		if (itemIdMisses.contains(normalized))
		{
			return null;
		}

		for (Integer itemId : item.getItemIds())
		{
			if (itemId != null && itemId > 0)
			{
				itemIdCache.put(normalized, itemId);
				return itemId;
			}
		}

		Integer resolved = findExactItemId(item.getName(), normalized);
		if (resolved != null)
		{
			itemIdCache.put(normalized, resolved);
		}
		return resolved;
	}

	private Integer findExactItemId(String name, String normalized)
	{
		if (itemManager == null)
		{
			return null;
		}
		try
		{
			List<ItemPrice> results = itemManager.search(name);
			if (results != null)
			{
				for (ItemPrice itemPrice : results)
				{
					if (itemPrice != null && itemPrice.getName() != null
						&& ItemResolver.normalize(itemPrice.getName()).equals(normalized))
					{
						return itemPrice.getId();
					}
				}
			}
			itemIdMisses.add(normalized);
		}
		catch (RuntimeException ignored)
		{
			// ItemManager may be unavailable while its price data is still initialising.
			// Do not permanently negative-cache transient failures.
		}
		return null;
	}
}
