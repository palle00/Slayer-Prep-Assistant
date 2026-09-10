package com.slayerprepassistant.gear;

import com.slayerprepassistant.bank.OwnershipState;
import com.slayerprepassistant.bank.PlayerInventoryState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;

public class LoadoutBuilder
{
	private final GearMatcher gearMatcher;

	public LoadoutBuilder(GearMatcher gearMatcher)
	{
		this.gearMatcher = gearMatcher == null ? new GearMatcher() : gearMatcher;
	}

	public LoadoutResult build(List<GearRecommendation> recommendations, PlayerInventoryState state, LoadoutMode mode)
	{
		if (recommendations == null || recommendations.isEmpty())
		{
			return new LoadoutResult(mode, Collections.emptyList());
		}
		List<GearMatch> matches = new ArrayList<>(recommendations.size());
		for (GearRecommendation recommendation : recommendations)
		{
			if (recommendation != null)
			{
				matches.add(selectForSlot(recommendation, state, mode));
			}
		}
		return new LoadoutResult(mode, matches);
	}

	private GearMatch selectForSlot(GearRecommendation recommendation, PlayerInventoryState state, LoadoutMode mode)
	{
		List<GearMatch> candidates = rankedCandidates(recommendation, state);
		if (candidates.isEmpty())
		{
			return new GearMatch(recommendation.getSlot(), new RecommendedItem("No recommendation"), OwnershipState.UNKNOWN, Integer.MAX_VALUE);
		}
		LoadoutMode effectiveMode = mode == null ? LoadoutMode.MAX : mode;
		if (effectiveMode == LoadoutMode.MAX)
		{
			return candidates.get(0);
		}
		for (GearMatch candidate : candidates)
		{
			if (isOwned(candidate.getOwnershipState()))
			{
				return candidate;
			}
		}
		return candidates.get(0);
	}

	private List<GearMatch> rankedCandidates(GearRecommendation recommendation, PlayerInventoryState state)
	{
		List<GearTier> tiers = recommendation.getTiers();
		if (tiers == null || tiers.isEmpty())
		{
			return Collections.emptyList();
		}
		List<GearTier> sortedTiers = new ArrayList<>();
		for (GearTier tier : tiers)
		{
			if (tier != null)
			{
				sortedTiers.add(tier);
			}
		}
		sortedTiers.sort(Comparator.comparingInt(GearTier::getPriority));

		List<GearMatch> candidates = new ArrayList<>();
		for (GearTier tier : sortedTiers)
		{
			for (RecommendedItem item : tier.getAlternatives())
			{
				if (item != null)
				{
					candidates.add(new GearMatch(recommendation.getSlot(), item, gearMatcher.ownershipFor(item, state), tier.getPriority()));
				}
			}
		}
		return candidates;
	}

	private boolean isOwned(OwnershipState state)
	{
		return state == OwnershipState.EQUIPPED || state == OwnershipState.OWNED_IN_INVENTORY || state == OwnershipState.OWNED_IN_BANK;
	}
}
