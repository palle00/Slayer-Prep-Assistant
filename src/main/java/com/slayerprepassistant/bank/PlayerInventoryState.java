package com.slayerprepassistant.bank;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerInventoryState
{
	private final Map<Integer, Integer> equipment;
	private final Map<Integer, Integer> inventory;
	private final Set<String> equipmentNames;
	private final Set<String> inventoryNames;
	private final BankSnapshot bankSnapshot;

	public PlayerInventoryState(Map<Integer, Integer> equipment, Map<Integer, Integer> inventory, BankSnapshot bankSnapshot)
	{
		this(equipment, inventory, Collections.emptySet(), Collections.emptySet(), bankSnapshot);
	}

	public PlayerInventoryState(Map<Integer, Integer> equipment, Map<Integer, Integer> inventory, Set<String> equipmentNames, Set<String> inventoryNames, BankSnapshot bankSnapshot)
	{
		this.equipment = Collections.unmodifiableMap(new HashMap<>(equipment));
		this.inventory = Collections.unmodifiableMap(new HashMap<>(inventory));
		this.equipmentNames = Collections.unmodifiableSet(new HashSet<>(equipmentNames));
		this.inventoryNames = Collections.unmodifiableSet(new HashSet<>(inventoryNames));
		this.bankSnapshot = bankSnapshot;
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

	public BankSnapshot getBankSnapshot()
	{
		return bankSnapshot;
	}
}
