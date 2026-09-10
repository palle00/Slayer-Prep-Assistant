package com.slayerprepassistant.prep;

import com.slayerprepassistant.bank.PlayerInventoryState;
import com.slayerprepassistant.gear.GearMatcher;
import com.slayerprepassistant.gear.GearRecommendation;
import com.slayerprepassistant.gear.GearSlot;
import com.slayerprepassistant.gear.GearTier;
import com.slayerprepassistant.gear.LoadoutBuilder;
import com.slayerprepassistant.gear.LoadoutMode;
import com.slayerprepassistant.gear.LoadoutResult;
import com.slayerprepassistant.gear.RecommendedItem;
import com.slayerprepassistant.guide.CombatMethod;
import com.slayerprepassistant.guide.InventoryRecommendation;
import com.slayerprepassistant.guide.MonsterGuide;
import com.slayerprepassistant.guide.StrategyMethod;
import com.slayerprepassistant.items.ItemResolver;
import com.slayerprepassistant.model.ParsingConfidence;
import com.slayerprepassistant.readiness.ReadinessResult;
import com.slayerprepassistant.readiness.ReadinessService;
import com.slayerprepassistant.task.SlayerTaskContext;
import com.slayerprepassistant.task.TargetOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PreparationEngine
{
	private final LoadoutBuilder loadoutBuilder;
	private final ReadinessService readinessService;

	public PreparationEngine(ItemResolver itemResolver)
	{
		GearMatcher gearMatcher = new GearMatcher();
		this.loadoutBuilder = new LoadoutBuilder(gearMatcher);
		this.readinessService = new ReadinessService(gearMatcher);
	}

	public PreparationResult prepareGuide(
		SlayerTaskContext taskContext,
		List<TargetOption> targets,
		TargetOption selectedTarget,
		MonsterGuide guide,
		CombatMethod preferredMethod,
		LoadoutMode loadoutMode,
		PlayerInventoryState playerState)
	{
		if (!hasUsableSetup(guide))
		{
			return PreparationResult.noSetup(taskContext, targets, selectedTarget, "A usable strategy setup could not be found for this monster.");
		}

		StrategyMethod strategyMethod = deduplicateStrategyMethod(chooseMethod(guide, preferredMethod));
		PlayerInventoryState effectiveState = playerState == null ? PlayerInventoryState.unknownBank() : playerState;
		List<GearRecommendation> gearRecommendations = strategyMethod.getGear();
		LoadoutResult loadout = loadoutBuilder.build(gearRecommendations, effectiveState, loadoutMode);
		List<InventoryRecommendation> inventoryRecommendations = strategyMethod.getInventory();
		ReadinessResult readiness = readinessService.evaluate(inventoryRecommendations, loadout, effectiveState);

		return new PreparationResult(taskContext, targets, selectedTarget, guide, strategyMethod.getMethod(), strategyMethod, loadout, readiness, effectiveState);
	}

	private boolean hasUsableSetup(MonsterGuide guide)
	{
		return guide != null
			&& guide.getMethods() != null
			&& guide.getMethods().stream().anyMatch(method -> method != null && method.getGear() != null && !method.getGear().isEmpty());
	}

	private StrategyMethod chooseMethod(MonsterGuide guide, CombatMethod preferredMethod)
	{
		if (guide == null || guide.getMethods() == null || guide.getMethods().isEmpty())
		{
			return emptyMethod();
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
		return emptyMethod();
	}

	private StrategyMethod deduplicateStrategyMethod(StrategyMethod method)
	{
		if (method == null)
		{
			return emptyMethod();
		}
		return new StrategyMethod(
			method.getMethod(),
			deduplicateGear(method.getGear()),
			deduplicateInventory(method.getInventory()),
			method.getNotes(),
			method.getConfidence());
	}

	private List<GearRecommendation> deduplicateGear(List<GearRecommendation> recommendations)
	{
		if (recommendations == null || recommendations.isEmpty())
		{
			return Collections.emptyList();
		}

		Map<GearSlot, Map<Integer, LinkedHashMap<String, RecommendedItem>>> bySlot = new EnumMap<>(GearSlot.class);
		for (GearRecommendation recommendation : recommendations)
		{
			if (recommendation == null || recommendation.getSlot() == null || recommendation.getTiers() == null)
			{
				continue;
			}
			Map<Integer, LinkedHashMap<String, RecommendedItem>> byPriority = bySlot.computeIfAbsent(recommendation.getSlot(), ignored -> new LinkedHashMap<>());
			for (GearTier tier : recommendation.getTiers())
			{
				if (tier == null || tier.getAlternatives() == null)
				{
					continue;
				}
				LinkedHashMap<String, RecommendedItem> items = byPriority.computeIfAbsent(tier.getPriority(), ignored -> new LinkedHashMap<>());
				for (RecommendedItem item : tier.getAlternatives())
				{
					if (item == null)
					{
						continue;
					}
					String key = ItemResolver.canonicalKey(item.getName());
					if (!key.isEmpty())
					{
						items.putIfAbsent(key, item);
					}
				}
			}
		}

		List<GearRecommendation> result = new ArrayList<>();
		for (Map.Entry<GearSlot, Map<Integer, LinkedHashMap<String, RecommendedItem>>> slotEntry : bySlot.entrySet())
		{
			List<GearTier> tiers = new ArrayList<>();
			for (Map.Entry<Integer, LinkedHashMap<String, RecommendedItem>> tierEntry : slotEntry.getValue().entrySet())
			{
				if (!tierEntry.getValue().isEmpty())
				{
					tiers.add(new GearTier(tierEntry.getKey(), new ArrayList<>(tierEntry.getValue().values())));
				}
			}
			tiers.sort(Comparator.comparingInt(GearTier::getPriority));
			if (!tiers.isEmpty())
			{
				result.add(new GearRecommendation(slotEntry.getKey(), tiers));
			}
		}
		return result;
	}

	private List<InventoryRecommendation> deduplicateInventory(List<InventoryRecommendation> recommendations)
	{
		if (recommendations == null || recommendations.isEmpty())
		{
			return Collections.emptyList();
		}

		Map<String, InventoryRecommendation> unique = new LinkedHashMap<>();
		for (InventoryRecommendation recommendation : recommendations)
		{
			if (recommendation == null)
			{
				continue;
			}
			String key = ItemResolver.canonicalKey(recommendation.getItemOrCategory());
			if (key.isEmpty())
			{
				continue;
			}
			InventoryRecommendation existing = unique.get(key);
			if (existing == null)
			{
				unique.put(key, recommendation);
			}
			else
			{
				unique.put(key, new InventoryRecommendation(
					existing.getItemOrCategory(),
					Math.max(existing.getMinimumQuantity(), recommendation.getMinimumQuantity()),
					existing.isRequired() || recommendation.isRequired()));
			}
		}
		return new ArrayList<>(unique.values());
	}

	private StrategyMethod emptyMethod()
	{
		return new StrategyMethod(
			CombatMethod.defaultMethod(),
			Collections.emptyList(),
			Collections.emptyList(),
			Collections.emptyList(),
			ParsingConfidence.UNKNOWN);
	}
}
