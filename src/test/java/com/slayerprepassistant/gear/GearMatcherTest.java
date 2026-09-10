package com.slayerprepassistant.gear;

import static org.junit.Assert.assertEquals;

import com.slayerprepassistant.bank.BankSnapshot;
import com.slayerprepassistant.bank.OwnershipState;
import com.slayerprepassistant.bank.PlayerInventoryState;
import com.slayerprepassistant.items.ItemResolver;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import org.junit.Test;

public class GearMatcherTest
{
	@Test
	public void matchesEquippedVariantByName()
	{
		PlayerInventoryState state = new PlayerInventoryState(
			Collections.emptyMap(),
			Collections.emptyMap(),
			new HashSet<>(Collections.singletonList("Slayer helmet (i)")),
			Collections.emptySet(),
			BankSnapshot.emptyUnknown());

		OwnershipState ownership = new GearMatcher(new ItemResolver()).ownershipFor(new RecommendedItem("Slayer helmet"), state);

		assertEquals(OwnershipState.EQUIPPED, ownership);
	}

	@Test
	public void matchesKnownBankVariantByName()
	{
		BankSnapshot bank = new BankSnapshot(Collections.emptyMap(), new HashSet<>(Collections.singletonList("Black mask (10)")), Instant.now());
		PlayerInventoryState state = new PlayerInventoryState(Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet(), Collections.emptySet(), bank);

		OwnershipState ownership = new GearMatcher(new ItemResolver()).ownershipFor(new RecommendedItem("Black mask"), state);

		assertEquals(OwnershipState.OWNED_IN_BANK, ownership);
	}

	@Test
	public void doesNotMatchDifferentNumberedUpgradeTiers()
	{
		BankSnapshot bank = new BankSnapshot(Collections.emptyMap(), new HashSet<>(Collections.singletonList("Rada's blessing 3")), Instant.now());
		PlayerInventoryState state = new PlayerInventoryState(Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet(), Collections.emptySet(), bank);

		OwnershipState ownership = new GearMatcher(new ItemResolver()).ownershipFor(new RecommendedItem("Rada's blessing 4"), state);

		assertEquals(OwnershipState.UNKNOWN, ownership);
	}

	@Test
	public void inventoryOwnershipIgnoresEquippedItems()
	{
		PlayerInventoryState state = new PlayerInventoryState(
			Collections.emptyMap(),
			Collections.emptyMap(),
			new HashSet<>(Collections.singletonList("Prayer potion")),
			Collections.emptySet(),
			new BankSnapshot(Collections.emptyMap(), Collections.emptySet(), Instant.now()));

		OwnershipState ownership = new GearMatcher(new ItemResolver()).ownershipForInventoryItem(new RecommendedItem("Prayer potion"), state);

		assertEquals(OwnershipState.MISSING, ownership);
	}
}
