package com.slayerprepassistant.readiness;

import com.slayerprepassistant.gear.GearMatch;
import com.slayerprepassistant.gear.LoadoutResult;
import com.slayerprepassistant.guide.InventoryRecommendation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReadinessService
{
	public ReadinessResult evaluate(List<InventoryRecommendation> inventory, LoadoutResult loadout)
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
			com.slayerprepassistant.bank.OwnershipState state = match.getOwnershipState();
			String slotName = match.getSlot().displayName() == null ? "" : match.getSlot().displayName();
			String itemName = match.getItem().getName() == null ? "" : match.getItem().getName();
			String displayString = slotName + ": " + itemName;

			if (state == null)
			{
				state = com.slayerprepassistant.bank.OwnershipState.UNKNOWN;
			}

			switch (state)
			{
				case EQUIPPED:
				case OWNED_IN_INVENTORY:
				case OWNED_IN_BANK:
					ready.add(displayString);
					break;
				case MISSING:
					warnings.add(new ReadinessIssue("Missing gear", displayString));
					score -= 10;
					break;
				case UNKNOWN:
				default:
					unknown.add(displayString);
					score -= 3;
					break;
			}
		}

		List<InventoryRecommendation> inventoryRecs = inventory == null ? Collections.emptyList() : inventory;
		for (InventoryRecommendation recommendation : inventoryRecs)
		{
			if (recommendation != null && recommendation.isRequired())
			{
				String itemOrCategory = recommendation.getItemOrCategory() == null ? "" : recommendation.getItemOrCategory();
				warnings.add(new ReadinessIssue("Inventory", itemOrCategory));
				score -= 7;
			}
		}

		return new ReadinessResult(Math.max(0, Math.min(100, score)), blockers, warnings, ready, unknown);
	}
}