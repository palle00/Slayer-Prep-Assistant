package com.slayerprepassistant.readiness;

import com.slayerprepassistant.bank.OwnershipState;
import com.slayerprepassistant.bank.PlayerInventoryState;
import com.slayerprepassistant.gear.GearMatch;
import com.slayerprepassistant.gear.GearMatcher;
import com.slayerprepassistant.gear.LoadoutResult;
import com.slayerprepassistant.gear.RecommendedItem;
import com.slayerprepassistant.guide.InventoryRecommendation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReadinessService
{
	private final GearMatcher gearMatcher;

	public ReadinessService()
	{
		this(new GearMatcher());
	}

	public ReadinessService(GearMatcher gearMatcher)
	{
		this.gearMatcher = gearMatcher == null ? new GearMatcher() : gearMatcher;
	}

	public ReadinessResult evaluate(List<InventoryRecommendation> inventory, LoadoutResult loadout)
	{
		return evaluate(inventory, loadout, PlayerInventoryState.unknownBank());
	}

	public ReadinessResult evaluate(List<InventoryRecommendation> inventory, LoadoutResult loadout, PlayerInventoryState playerState)
	{
		int score = 100;
		List<ReadinessIssue> blockers = new ArrayList<>();
		List<ReadinessIssue> warnings = new ArrayList<>();
		List<String> ready = new ArrayList<>();
		List<String> unknown = new ArrayList<>();

		List<GearMatch> gearMatches = loadout == null || loadout.getGearMatches() == null
			? Collections.emptyList()
			: loadout.getGearMatches();
		for (GearMatch match : gearMatches)
		{
			if (match == null || match.getSlot() == null || match.getItem() == null)
			{
				continue;
			}
			OwnershipState state = match.getOwnershipState() == null ? OwnershipState.UNKNOWN : match.getOwnershipState();
			String displayString = match.getSlot().displayName() + ": " + match.getItem().getName();
			if (isReady(state))
			{
				ready.add(displayString);
			}
			else if (state == OwnershipState.MISSING)
			{
				warnings.add(new ReadinessIssue("Missing gear", displayString));
				score -= 10;
			}
			else
			{
				unknown.add(displayString);
				score -= 3;
			}
		}

		PlayerInventoryState effectiveState = playerState == null ? PlayerInventoryState.unknownBank() : playerState;
		for (InventoryRecommendation recommendation : inventory == null ? Collections.<InventoryRecommendation>emptyList() : inventory)
		{
			if (recommendation == null || !recommendation.isRequired())
			{
				continue;
			}
			String itemName = recommendation.getItemOrCategory();
			int requiredQuantity = Math.max(1, recommendation.getMinimumQuantity());
			OwnershipState state = gearMatcher.ownershipForInventoryItem(new RecommendedItem(itemName), effectiveState, requiredQuantity);
			String displayString = requiredQuantity > 1 ? requiredQuantity + " x " + itemName : itemName;
			if (isReady(state))
			{
				ready.add("Inventory: " + displayString);
			}
			else if (state == OwnershipState.MISSING)
			{
				warnings.add(new ReadinessIssue("Missing inventory", displayString));
				score -= 7;
			}
			else
			{
				unknown.add("Inventory: " + displayString);
				score -= 3;
			}
		}

		return new ReadinessResult(Math.max(0, Math.min(100, score)), blockers, warnings, ready, unknown);
	}

	private boolean isReady(OwnershipState state)
	{
		return state == OwnershipState.EQUIPPED || state == OwnershipState.OWNED_IN_INVENTORY || state == OwnershipState.OWNED_IN_BANK;
	}
}
