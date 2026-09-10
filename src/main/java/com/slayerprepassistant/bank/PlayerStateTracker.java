package com.slayerprepassistant.bank;

import net.runelite.api.Client;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;

public class PlayerStateTracker
{
	private final BankSnapshotService bankSnapshotService = new BankSnapshotService();
	private PlayerInventoryState state = PlayerInventoryState.unknownBank();
	private long stateRevision;

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
		BankSnapshotService.ContainerSnapshot equipmentSnapshot = BankSnapshotService.capture(equipment, client, false);
		BankSnapshotService.ContainerSnapshot inventorySnapshot = BankSnapshotService.capture(inventory, client, false);
		PlayerInventoryState nextState = new PlayerInventoryState(
			equipmentSnapshot.quantitiesById,
			inventorySnapshot.quantitiesById,
			equipmentSnapshot.itemNames,
			inventorySnapshot.itemNames,
			equipmentSnapshot.quantitiesByCanonicalName,
			inventorySnapshot.quantitiesByCanonicalName,
			bankSnapshotService.getSnapshot());

		if (nextState.equals(state))
		{
			return false;
		}
		state = nextState;
		stateRevision++;
		return true;
	}

	public PlayerInventoryState getState()
	{
		return state;
	}

	public String getStateKey()
	{
		return Long.toString(stateRevision);
	}
}
