package com.slayerprepassistant.bank;

import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import net.runelite.api.Client;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;

public class PlayerStateTracker
{
	private final BankSnapshotService bankSnapshotService = new BankSnapshotService();
	private PlayerInventoryState state = PlayerInventoryState.unknownBank();
	private String stateKey = "";

	public void updateBank(ItemContainer container, Client client)
	{
		bankSnapshotService.updateFromBankContainer(container, client);
	}

	public boolean update(Client client)
	{
		if (client == null)
		{
			return false;
		}
		ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		PlayerInventoryState nextState = new PlayerInventoryState(
				BankSnapshotService.toQuantities(equipment),
				BankSnapshotService.toQuantities(inventory),
				BankSnapshotService.toNames(equipment, client),
				BankSnapshotService.toNames(inventory, client),
				bankSnapshotService.getSnapshot());
		String nextStateKey = key(nextState);
		if (nextStateKey.equals(stateKey))
		{
			return false;
		}
		state = nextState;
		stateKey = nextStateKey;
		return true;
	}

	public PlayerInventoryState getState()
	{
		return state;
	}

	public String getStateKey()
	{
		return stateKey;
	}

	private String key(PlayerInventoryState state)
	{
		return Objects.toString(new TreeMap<>(state.getEquipment()))
				+ "|" + Objects.toString(new TreeMap<>(state.getInventory()))
				+ "|" + Objects.toString(new TreeSet<>(state.getEquipmentNames()))
				+ "|" + Objects.toString(new TreeSet<>(state.getInventoryNames()))
				+ "|bankKnown=" + state.getBankSnapshot().isKnown()
				+ "|" + Objects.toString(new TreeMap<>(state.getBankSnapshot().getQuantitiesById()))
				+ "|" + Objects.toString(new TreeSet<>(state.getBankSnapshot().getItemNames()));
	}
}