package com.slayerprepassistant.guide;

import com.slayerprepassistant.gear.GearRecommendation;
import com.slayerprepassistant.model.ParsingConfidence;
import java.util.Collections;
import java.util.List;

public class StrategyMethod
{
	private final CombatMethod method;
	private final List<GearRecommendation> gear;
	private final List<InventoryRecommendation> inventory;
	private final List<String> notes;
	private final ParsingConfidence confidence;

	public StrategyMethod(CombatMethod method, List<GearRecommendation> gear, List<InventoryRecommendation> inventory, List<String> notes, ParsingConfidence confidence)
	{
		this.method = method;
		this.gear = Collections.unmodifiableList(gear);
		this.inventory = Collections.unmodifiableList(inventory);
		this.notes = Collections.unmodifiableList(notes);
		this.confidence = confidence;
	}

	public CombatMethod getMethod()
	{
		return method;
	}

	public List<GearRecommendation> getGear()
	{
		return gear;
	}

	public List<InventoryRecommendation> getInventory()
	{
		return inventory;
	}

	public List<String> getNotes()
	{
		return notes;
	}

	public ParsingConfidence getConfidence()
	{
		return confidence;
	}
}
