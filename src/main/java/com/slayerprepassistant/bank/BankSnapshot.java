package com.slayerprepassistant.bank;

import com.slayerprepassistant.items.ItemResolver;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class BankSnapshot
{
	private final Map<Integer, Integer> quantitiesById;
	private final Set<String> itemNames;
	private final Map<String, Integer> quantitiesByCanonicalName;
	private final Instant capturedAt;

	public BankSnapshot(Map<Integer, Integer> quantitiesById, Instant capturedAt)
	{
		this(quantitiesById, Collections.emptySet(), Collections.emptyMap(), capturedAt);
	}

	public BankSnapshot(Map<Integer, Integer> quantitiesById, Set<String> itemNames, Instant capturedAt)
	{
		this(quantitiesById, itemNames, quantitiesForNames(itemNames), capturedAt);
	}

	BankSnapshot(Map<Integer, Integer> quantitiesById, Set<String> itemNames, Map<String, Integer> quantitiesByCanonicalName, Instant capturedAt)
	{
		this.quantitiesById = immutableMap(quantitiesById);
		this.itemNames = itemNames == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(itemNames));
		this.quantitiesByCanonicalName = immutableStringMap(quantitiesByCanonicalName);
		this.capturedAt = capturedAt;
	}

	public static BankSnapshot emptyUnknown()
	{
		return new BankSnapshot(Collections.emptyMap(), Collections.emptySet(), Collections.emptyMap(), null);
	}

	public Map<Integer, Integer> getQuantitiesById()
	{
		return quantitiesById;
	}

	public Set<String> getItemNames()
	{
		return itemNames;
	}

	public Set<String> getCanonicalItemNames()
	{
		return quantitiesByCanonicalName.keySet();
	}

	public boolean isKnown()
	{
		return capturedAt != null;
	}

	public boolean containsAny(Iterable<Integer> itemIds)
	{
		return quantityForAny(itemIds) > 0;
	}

	public int quantityForAny(Iterable<Integer> itemIds)
	{
		if (itemIds == null)
		{
			return 0;
		}
		int quantity = 0;
		for (Integer itemId : itemIds)
		{
			if (itemId != null)
			{
				quantity += Math.max(0, quantitiesById.getOrDefault(itemId, 0));
			}
		}
		return quantity;
	}

	public int quantityForCanonicalName(String canonicalName)
	{
		return canonicalName == null ? 0 : quantitiesByCanonicalName.getOrDefault(canonicalName, 0);
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		if (!(other instanceof BankSnapshot))
		{
			return false;
		}
		BankSnapshot that = (BankSnapshot) other;
		return isKnown() == that.isKnown()
			&& quantitiesById.equals(that.quantitiesById)
			&& quantitiesByCanonicalName.equals(that.quantitiesByCanonicalName);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(quantitiesById, quantitiesByCanonicalName, isKnown());
	}

	private static Map<String, Integer> quantitiesForNames(Set<String> names)
	{
		if (names == null || names.isEmpty())
		{
			return Collections.emptyMap();
		}
		Map<String, Integer> quantities = new HashMap<>();
		for (String name : names)
		{
			String key = ItemResolver.canonicalKey(name);
			if (!key.isEmpty())
			{
				quantities.merge(key, 1, Integer::sum);
			}
		}
		return quantities;
	}

	private static Map<Integer, Integer> immutableMap(Map<Integer, Integer> source)
	{
		return source == null || source.isEmpty()
			? Collections.emptyMap()
			: Collections.unmodifiableMap(new HashMap<>(source));
	}

	private static Map<String, Integer> immutableStringMap(Map<String, Integer> source)
	{
		return source == null || source.isEmpty()
			? Collections.emptyMap()
			: Collections.unmodifiableMap(new HashMap<>(source));
	}
}
