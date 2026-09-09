package com.slayerprepassistant.bank;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BankSnapshot
{
	private final Map<Integer, Integer> quantitiesById;
	private final Set<String> itemNames;
	private final Instant capturedAt;

	public BankSnapshot(Map<Integer, Integer> quantitiesById, Instant capturedAt)
	{
		this(quantitiesById, Collections.emptySet(), capturedAt);
	}

	public BankSnapshot(Map<Integer, Integer> quantitiesById, Set<String> itemNames, Instant capturedAt)
	{
		this.quantitiesById = Collections.unmodifiableMap(new HashMap<>(quantitiesById));
		this.itemNames = Collections.unmodifiableSet(new HashSet<>(itemNames));
		this.capturedAt = capturedAt;
	}

	public static BankSnapshot emptyUnknown()
	{
		return new BankSnapshot(Collections.emptyMap(), null);
	}

	public Map<Integer, Integer> getQuantitiesById()
	{
		return quantitiesById;
	}

	public Set<String> getItemNames()
	{
		return itemNames;
	}

	public Instant getCapturedAt()
	{
		return capturedAt;
	}

	public boolean isKnown()
	{
		return capturedAt != null;
	}

	public boolean containsAny(Iterable<Integer> itemIds)
	{
		for (Integer itemId : itemIds)
		{
			if (quantitiesById.getOrDefault(itemId, 0) > 0)
			{
				return true;
			}
		}
		return false;
	}
}
