package com.slayerprepassistant.gear;

import com.slayerprepassistant.bank.OwnershipState;
import com.slayerprepassistant.bank.PlayerInventoryState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;

public class LoadoutBuilder
{
	private final GearMatcher gearMatcher;
	private final PriceLookup priceLookup;

	public LoadoutBuilder(GearMatcher gearMatcher)
	{
		this(gearMatcher, PriceLookup.unavailable());
	}

	public LoadoutBuilder(GearMatcher gearMatcher, PriceLookup priceLookup)
	{
		this.gearMatcher = gearMatcher;
		this.priceLookup = priceLookup;
	}

	public LoadoutResult build(List<GearRecommendation> recommendations, PlayerInventoryState state, LoadoutMode mode)
	{
		List<GearMatch> matches = new ArrayList<>();
		for (GearRecommendation recommendation : recommendations)
		{
			matches.add(selectForSlot(recommendation, state, mode));
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
		if (mode == LoadoutMode.MAX)
		{
			return candidates.get(0);
		}
		if (mode == LoadoutMode.BEST_VALUE)
		{
			return bestValue(candidates);
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
		List<GearTier> tiers = new ArrayList<>(recommendation.getTiers());
		tiers.sort(Comparator.comparingInt(GearTier::getPriority));
		List<GearMatch> candidates = new ArrayList<>();
		for (GearTier tier : tiers)
		{
			for (RecommendedItem item : tier.getAlternatives())
			{
				candidates.add(new GearMatch(recommendation.getSlot(), item, gearMatcher.ownershipFor(item, state), tier.getPriority()));
			}
		}
		return candidates;
	}

	private GearMatch bestValue(List<GearMatch> candidates)
	{
		/*
		 * Best Value is intentionally not a DPS calculation. Wiki rank defines quality;
		 * price only decides which higher-ranked upgrade is the lowest-cost move above
		 * the player's best owned option. If prices are unavailable, the mode falls
		 * back to the owned baseline, or to the top Wiki recommendation when nothing
		 * is known to be owned.
		 */
		GearMatch ownedBaseline = null;
		for (GearMatch candidate : candidates)
		{
			if (isOwned(candidate.getOwnershipState()))
			{
				ownedBaseline = candidate;
				break;
			}
		}
		int baselineRank = ownedBaseline == null ? Integer.MAX_VALUE : ownedBaseline.getTierPriority();
		GearMatch cheapestUpgrade = null;
		int cheapestPrice = Integer.MAX_VALUE;
		for (GearMatch candidate : candidates)
		{
			if (candidate.getTierPriority() >= baselineRank)
			{
				continue;
			}
			OptionalInt price = priceLookup.price(candidate.getItem());
			if (price.isPresent() && price.getAsInt() > 0 && price.getAsInt() < cheapestPrice)
			{
				cheapestUpgrade = candidate;
				cheapestPrice = price.getAsInt();
			}
		}
		if (cheapestUpgrade != null)
		{
			return cheapestUpgrade;
		}
		if (ownedBaseline != null)
		{
			return ownedBaseline;
		}
		return cheapestPriced(candidates);
	}

	private GearMatch cheapestPriced(List<GearMatch> candidates)
	{
		GearMatch cheapest = null;
		int cheapestPrice = Integer.MAX_VALUE;
		for (GearMatch candidate : candidates)
		{
			OptionalInt price = priceLookup.price(candidate.getItem());
			if (price.isPresent() && price.getAsInt() > 0 && price.getAsInt() < cheapestPrice)
			{
				cheapest = candidate;
				cheapestPrice = price.getAsInt();
			}
		}
		return cheapest == null ? candidates.get(0) : cheapest;
	}

	private boolean isOwned(OwnershipState state)
	{
		return state == OwnershipState.EQUIPPED || state == OwnershipState.OWNED_IN_INVENTORY || state == OwnershipState.OWNED_IN_BANK;
	}
}
