package com.slayerprepassistant.bank;

import com.slayerprepassistant.items.ItemResolver;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class PlayerInventoryState
{
	private final Map<Integer, Integer> equipment;
	private final Map<Integer, Integer> inventory;
	private final Set<String> equipmentNames;
	private final Set<String> inventoryNames;
	private final Map<String, Integer> equipmentQuantitiesByCanonicalName;
	private final Map<String, Integer> inventoryQuantitiesByCanonicalName;
	private final BankSnapshot bankSnapshot;

	public PlayerInventoryState(Map<Integer, Integer> equipment, Map<Integer, Integer> inventory, BankSnapshot bankSnapshot)
	{
		this(equipment, inventory, Collections.emptySet(), Collections.emptySet(), Collections.emptyMap(), Collections.emptyMap(), bankSnapshot);
	}

	public PlayerInventoryState(Map<Integer, Integer> equipment, Map<Integer, Integer> inventory, Set<String> equipmentNames, Set<String> inventoryNames, BankSnapshot bankSnapshot)
	{
		this(equipment, inventory, equipmentNames, inventoryNames, quantitiesForNames(equipmentNames), quantitiesForNames(inventoryNames), bankSnapshot);
	}

	PlayerInventoryState(
		Map<Integer, Integer> equipment,
		Map<Integer, Integer> inventory,
		Set<String> equipmentNames,
		Set<String> inventoryNames,
		Map<String, Integer> equipmentQuantitiesByCanonicalName,
		Map<String, Integer> inventoryQuantitiesByCanonicalName,
		BankSnapshot bankSnapshot)
	{
		this.equipment = immutableIntMap(equipment);
		this.inventory = immutableIntMap(inventory);
		this.equipmentNames = immutableSet(equipmentNames);
		this.inventoryNames = immutableSet(inventoryNames);
		this.equipmentQuantitiesByCanonicalName = immutableStringMap(equipmentQuantitiesByCanonicalName);
		this.inventoryQuantitiesByCanonicalName = immutableStringMap(inventoryQuantitiesByCanonicalName);
		this.bankSnapshot = bankSnapshot == null ? BankSnapshot.emptyUnknown() : bankSnapshot;
	}

	public static PlayerInventoryState unknownBank()
	{
		return new PlayerInventoryState(Collections.emptyMap(), Collections.emptyMap(), BankSnapshot.emptyUnknown());
	}

	public Map<Integer, Integer> getEquipment()
	{
		return equipment;
	}

	public Map<Integer, Integer> getInventory()
	{
		return inventory;
	}

	public Set<String> getEquipmentNames()
	{
		return equipmentNames;
	}

	public Set<String> getInventoryNames()
	{
		return inventoryNames;
	}

	public Set<String> getEquipmentCanonicalNames()
	{
		return equipmentQuantitiesByCanonicalName.keySet();
	}

	public Set<String> getInventoryCanonicalNames()
	{
		return inventoryQuantitiesByCanonicalName.keySet();
	}

	public int equipmentQuantityForCanonicalName(String canonicalName)
	{
		return canonicalName == null ? 0 : equipmentQuantitiesByCanonicalName.getOrDefault(canonicalName, 0);
	}

	public int inventoryQuantityForCanonicalName(String canonicalName)
	{
		return canonicalName == null ? 0 : inventoryQuantitiesByCanonicalName.getOrDefault(canonicalName, 0);
	}

	public BankSnapshot getBankSnapshot()
	{
		return bankSnapshot;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		if (!(other instanceof PlayerInventoryState))
		{
			return false;
		}
		PlayerInventoryState that = (PlayerInventoryState) other;
		return equipment.equals(that.equipment)
			&& inventory.equals(that.inventory)
			&& equipmentQuantitiesByCanonicalName.equals(that.equipmentQuantitiesByCanonicalName)
			&& inventoryQuantitiesByCanonicalName.equals(that.inventoryQuantitiesByCanonicalName)
			&& bankSnapshot.equals(that.bankSnapshot);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(equipment, inventory, equipmentQuantitiesByCanonicalName, inventoryQuantitiesByCanonicalName, bankSnapshot);
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

	private static Map<Integer, Integer> immutableIntMap(Map<Integer, Integer> source)
	{
		return source == null || source.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(source));
	}

	private static Map<String, Integer> immutableStringMap(Map<String, Integer> source)
	{
		return source == null || source.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(source));
	}

	private static Set<String> immutableSet(Set<String> source)
	{
		return source == null || source.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(source));
	}
}
