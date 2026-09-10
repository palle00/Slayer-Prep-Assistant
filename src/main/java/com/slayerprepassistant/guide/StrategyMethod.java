package com.slayerprepassistant.guide;

import com.slayerprepassistant.gear.GearRecommendation;
import com.slayerprepassistant.model.ParsingConfidence;
import java.util.ArrayList;
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
		this.method = method == null ? CombatMethod.defaultMethod() : method;
		this.gear = gear == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(gear));
		this.inventory = inventory == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(inventory));
		this.notes = notes == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(notes));
		this.confidence = confidence == null ? ParsingConfidence.UNKNOWN : confidence;
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