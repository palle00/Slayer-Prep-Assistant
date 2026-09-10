package com.slayerprepassistant.items;

import com.slayerprepassistant.gear.RecommendedItem;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import net.runelite.client.game.ItemManager;
import net.runelite.http.api.item.ItemPrice;

public class RuneLiteItemLookup
{
	private final ItemManager itemManager;
	private final Map<String, Integer> itemIdCache = new HashMap<>();
	private final Map<String, OptionalInt> priceCache = new HashMap<>();
	private final Map<String, ItemPrice> searchCache = new HashMap<>();
	private final Set<String> searchMisses = new HashSet<>();

	public RuneLiteItemLookup(ItemManager itemManager)
	{
		this.itemManager = itemManager;
	}

	public Integer itemId(RecommendedItem item)
	{
		if (item == null || item.getName().trim().isEmpty())
		{
			return null;
		}
		String normalized = ItemResolver.normalize(item.getName());
		if (itemIdCache.containsKey(normalized))
		{
			return itemIdCache.get(normalized);
		}
		if (!item.getItemIds().isEmpty())
		{
			Integer itemId = item.getItemIds().iterator().next();
			itemIdCache.put(normalized, itemId);
			return itemId;
		}
		ItemPrice match = findExact(item.getName(), normalized);
		Integer itemId = match == null ? null : match.getId();
		itemIdCache.put(normalized, itemId);
		return itemId;
	}

	public OptionalInt wikiPrice(RecommendedItem item)
	{
		if (item == null || item.getName().trim().isEmpty())
		{
			return OptionalInt.empty();
		}
		String normalized = ItemResolver.normalize(item.getName());
		OptionalInt cached = priceCache.get(normalized);
		if (cached != null)
		{
			return cached;
		}
		ItemPrice match = findExact(item.getName(), normalized);
		int price = match == null ? 0 : match.getWikiPrice() > 0 ? match.getWikiPrice() : match.getPrice();
		OptionalInt result = price > 0 ? OptionalInt.of(price) : OptionalInt.empty();
		priceCache.put(normalized, result);
		return result;
	}

	private ItemPrice findExact(String name, String normalized)
	{
		if (searchCache.containsKey(normalized))
		{
			return searchCache.get(normalized);
		}
		if (searchMisses.contains(normalized) || itemManager == null)
		{
			return null;
		}
		try
		{
			for (ItemPrice itemPrice : itemManager.search(name))
			{
				if (ItemResolver.normalize(itemPrice.getName()).equals(normalized))
				{
					searchCache.put(normalized, itemPrice);
					return itemPrice;
				}
			}
		}
		catch (RuntimeException ex)
		{
			searchMisses.add(normalized);
			return null;
		}
		searchMisses.add(normalized);
		return null;
	}
}
