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
	private final ReadinessService readinessService = new ReadinessService();

	public PreparationEngine()
	{
		this(new ItemResolver(), PriceLookup.unavailable());
	}

	public PreparationEngine(ItemResolver itemResolver, PriceLookup priceLookup)
	{
		this.loadoutBuilder = new LoadoutBuilder(new GearMatcher(itemResolver), priceLookup);
	}

	public PreparationResult prepare(SlayerTaskContext taskContext, List<TargetOption> targets, TargetOption selectedTarget, CombatMethod preferredMethod, LoadoutMode loadoutMode, PlayerInventoryState playerState)
	{
		return PreparationResult.noSetup(taskContext, targets, selectedTarget, "No setup found.");
	}

	public PreparationResult prepareGuide(SlayerTaskContext taskContext, List<TargetOption> targets, TargetOption selectedTarget, MonsterGuide guide, CombatMethod preferredMethod, LoadoutMode loadoutMode, PlayerInventoryState playerState)
	{
		if (!hasUsableSetup(guide))
		{
			return PreparationResult.noSetup(taskContext, targets, selectedTarget, "A usable strategy setup could not be found for this monster.");
		}
		StrategyMethod strategyMethod = chooseMethod(guide, preferredMethod);
		com.slayerprepassistant.gear.LoadoutResult loadout = loadoutBuilder.build(strategyMethod.getGear(), playerState, loadoutMode);
		ReadinessResult readiness = readinessService.evaluate(strategyMethod.getInventory(), loadout);
		return new PreparationResult(taskContext, targets, selectedTarget, guide, strategyMethod.getMethod(), strategyMethod, loadout, readiness, playerState);
	}

	private boolean hasUsableSetup(MonsterGuide guide)
	{
		return guide != null && guide.getMethods().stream().anyMatch(method -> !method.getGear().isEmpty());
	}

	private StrategyMethod chooseMethod(MonsterGuide guide, CombatMethod preferredMethod)
	{
		for (StrategyMethod method : guide.getMethods())
		{
			if (method.getMethod() == preferredMethod)
			{
				return method;
			}
		}
		return guide.getMethods().isEmpty()
			? new StrategyMethod(CombatMethod.GENERAL, java.util.Collections.emptyList(), java.util.Collections.emptyList(), java.util.Collections.emptyList(), com.slayerprepassistant.model.ParsingConfidence.UNKNOWN)
			: guide.getMethods().get(0);
	}
}
