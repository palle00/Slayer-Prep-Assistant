package com.slayerprepassistant.gear;

import static org.junit.Assert.assertEquals;

import com.slayerprepassistant.bank.BankSnapshot;
import com.slayerprepassistant.bank.OwnershipState;
import com.slayerprepassistant.bank.PlayerInventoryState;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.OptionalInt;
import org.junit.Test;

public class LoadoutBuilderTest
{
	private final LoadoutBuilder builder = new LoadoutBuilder(new GearMatcher());

	@Test
	public void bestIOwnChoosesHighestRankedOwnedAlternative()
	{
		RecommendedItem best = new RecommendedItem("Best sword", new HashSet<>(Collections.singletonList(1)));
		RecommendedItem owned = new RecommendedItem("Owned sword", new HashSet<>(Collections.singletonList(2)));
		GearRecommendation recommendation = new GearRecommendation(GearSlot.WEAPON, Arrays.asList(
			new GearTier(1, Collections.singletonList(best)),
			new GearTier(2, Collections.singletonList(owned))));
		HashMap<Integer, Integer> bank = new HashMap<>();
		bank.put(2, 1);

		LoadoutResult result = builder.build(Collections.singletonList(recommendation),
			new PlayerInventoryState(Collections.emptyMap(), Collections.emptyMap(), new BankSnapshot(bank, Instant.now())),
			LoadoutMode.BEST_I_OWN);

		assertEquals("Owned sword", result.getGearMatches().get(0).getItem().getName());
		assertEquals(OwnershipState.OWNED_IN_BANK, result.getGearMatches().get(0).getOwnershipState());
	}

	@Test
	public void unknownBankDoesNotMarkRecommendedItemMissing()
	{
		RecommendedItem item = new RecommendedItem("Known id", new HashSet<>(Collections.singletonList(99)));
		GearRecommendation recommendation = new GearRecommendation(GearSlot.HEAD, Collections.singletonList(new GearTier(1, Collections.singletonList(item))));

		LoadoutResult result = builder.build(Collections.singletonList(recommendation), PlayerInventoryState.unknownBank(), LoadoutMode.BEST_I_OWN);

		assertEquals(OwnershipState.UNKNOWN, result.getGearMatches().get(0).getOwnershipState());
	}

	@Test
	public void maxChoosesHighestRankedWikiRecommendationEvenWhenMissing()
	{
		RecommendedItem best = new RecommendedItem("Best sword", new HashSet<>(Collections.singletonList(1)));
		RecommendedItem owned = new RecommendedItem("Owned sword", new HashSet<>(Collections.singletonList(2)));
		GearRecommendation recommendation = new GearRecommendation(GearSlot.WEAPON, Arrays.asList(
			new GearTier(1, Collections.singletonList(best)),
			new GearTier(2, Collections.singletonList(owned))));
		HashMap<Integer, Integer> bank = new HashMap<>();
		bank.put(2, 1);

		LoadoutResult result = builder.build(Collections.singletonList(recommendation),
			new PlayerInventoryState(Collections.emptyMap(), Collections.emptyMap(), new BankSnapshot(bank, Instant.now())),
			LoadoutMode.MAX);

		assertEquals("Best sword", result.getGearMatches().get(0).getItem().getName());
		assertEquals(OwnershipState.MISSING, result.getGearMatches().get(0).getOwnershipState());
	}
}
