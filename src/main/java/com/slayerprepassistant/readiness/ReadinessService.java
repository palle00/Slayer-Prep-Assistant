package com.slayerprepassistant.readiness;

import com.slayerprepassistant.gear.GearMatch;
import com.slayerprepassistant.gear.LoadoutResult;
import com.slayerprepassistant.guide.InventoryRecommendation;
import java.util.ArrayList;
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

		for (GearMatch match : loadout.getGearMatches())
		{
			switch (match.getOwnershipState())
			{
				case EQUIPPED:
				case OWNED_IN_INVENTORY:
				case OWNED_IN_BANK:
					ready.add(match.getSlot().displayName() + ": " + match.getItem().getName());
					break;
				case MISSING:
					warnings.add(new ReadinessIssue("Missing gear", match.getSlot().displayName() + ": " + match.getItem().getName()));
					score -= 10;
					break;
				case UNKNOWN:
				default:
					unknown.add(match.getSlot().displayName() + ": " + match.getItem().getName());
					score -= 3;
					break;
			}
		}

		for (InventoryRecommendation recommendation : inventory)
		{
			if (recommendation.isRequired())
			{
				warnings.add(new ReadinessIssue("Inventory", recommendation.getItemOrCategory()));
				score -= 7;
			}
		}

		return new ReadinessResult(Math.max(0, Math.min(100, score)), blockers, warnings, ready, unknown);
	}
}
