package com.slayerprepassistant.readiness;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.slayerprepassistant.bank.OwnershipState;
import com.slayerprepassistant.gear.GearMatch;
import com.slayerprepassistant.gear.GearSlot;
import com.slayerprepassistant.gear.LoadoutMode;
import com.slayerprepassistant.gear.LoadoutResult;
import com.slayerprepassistant.gear.RecommendedItem;
import java.util.Collections;
import org.junit.Test;

public class ReadinessServiceTest
{
	@Test
	public void unknownGearReducesReadinessWithoutMarkingMissing()
	{
		ReadinessResult result = new ReadinessService().evaluate(
			Collections.emptyList(),
			new LoadoutResult(LoadoutMode.BEST_I_OWN, Collections.singletonList(new GearMatch(GearSlot.WEAPON, new RecommendedItem("Leaf-bladed sword"), OwnershipState.UNKNOWN, 1))));

		assertEquals(97, result.getPercentage());
		assertFalse(result.getUnknownItems().isEmpty());
	}
}
