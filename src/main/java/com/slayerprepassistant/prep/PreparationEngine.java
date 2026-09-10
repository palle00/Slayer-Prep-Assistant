package com.slayerprepassistant.prep;

import com.slayerprepassistant.bank.PlayerInventoryState;
import com.slayerprepassistant.gear.GearMatcher;
import com.slayerprepassistant.gear.LoadoutBuilder;
import com.slayerprepassistant.gear.LoadoutMode;
import com.slayerprepassistant.gear.PriceLookup;
import com.slayerprepassistant.guide.CombatMethod;
import com.slayerprepassistant.guide.MonsterGuide;
import com.slayerprepassistant.guide.StrategyMethod;
import com.slayerprepassistant.items.ItemResolver;
import com.slayerprepassistant.readiness.ReadinessResult;
import com.slayerprepassistant.readiness.ReadinessService;
import com.slayerprepassistant.task.SlayerTaskContext;
import com.slayerprepassistant.task.TargetOption;
import java.util.List;

public class PreparationEngine
{
	private final LoadoutBuilder loadoutBuilder;
	private final ReadinessService readinessService;

	public PreparationEngine(ItemResolver itemResolver, PriceLookup priceLookup)
	{
		this.loadoutBuilder = new LoadoutBuilder(new GearMatcher(itemResolver), priceLookup);
		this.readinessService = new ReadinessService();
	}


	public PreparationResult prepareGuide(SlayerTaskContext taskContext, List<TargetOption> targets, TargetOption selectedTarget, MonsterGuide guide, CombatMethod preferredMethod, LoadoutMode loadoutMode, PlayerInventoryState playerState)
	{
		if (!hasUsableSetup(guide))
		{
			return PreparationResult.noSetup(taskContext, targets, selectedTarget, "A usable strategy setup could not be found for this monster.");
		}
		StrategyMethod strategyMethod = chooseMethod(guide, preferredMethod);
		List gearRecs = strategyMethod.getGear() == null ? java.util.Collections.emptyList() : strategyMethod.getGear();
		com.slayerprepassistant.gear.LoadoutResult loadout = loadoutBuilder.build(gearRecs, playerState, loadoutMode);

		List inventoryRecs = strategyMethod.getInventory() == null ? java.util.Collections.emptyList() : strategyMethod.getInventory();
		ReadinessResult readiness = readinessService.evaluate(inventoryRecs, loadout);

		return new PreparationResult(taskContext, targets, selectedTarget, guide, strategyMethod.getMethod(), strategyMethod, loadout, readiness, playerState);
	}

	private boolean hasUsableSetup(MonsterGuide guide)
	{
		if (guide == null || guide.getMethods() == null)
		{
			return false;
		}
		return guide.getMethods().stream().anyMatch(method -> method != null && method.getGear() != null && !method.getGear().isEmpty());
	}

	private StrategyMethod chooseMethod(MonsterGuide guide, CombatMethod preferredMethod)
	{
		if (guide == null || guide.getMethods() == null || guide.getMethods().isEmpty())
		{
			return new StrategyMethod(CombatMethod.defaultMethod(), java.util.Collections.emptyList(), java.util.Collections.emptyList(), java.util.Collections.emptyList(), com.slayerprepassistant.model.ParsingConfidence.UNKNOWN);
		}

		CombatMethod effectivePreferred = preferredMethod == null ? CombatMethod.defaultMethod() : preferredMethod;
		for (StrategyMethod method : guide.getMethods())
		{
			if (method != null && method.getMethod() == effectivePreferred)
			{
				return method;
			}
		}

		for (StrategyMethod method : guide.getMethods())
		{
			if (method != null)
			{
				return method;
			}
		}

		return new StrategyMethod(CombatMethod.defaultMethod(), java.util.Collections.emptyList(), java.util.Collections.emptyList(), java.util.Collections.emptyList(), com.slayerprepassistant.model.ParsingConfidence.UNKNOWN);
	}
}