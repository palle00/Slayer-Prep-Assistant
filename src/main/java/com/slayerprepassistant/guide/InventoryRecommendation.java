package com.slayerprepassistant.guide;

import java.util.Collections;
import java.util.List;

public class InventoryRecommendation
{
	private final String itemOrCategory;
	private final QuantityMode quantityMode;
	private final int minimumQuantity;
	private final int maximumQuantity;
	private final boolean required;
	private final String notes;
	private final List<String> alternatives;

	public InventoryRecommendation(String itemOrCategory, QuantityMode quantityMode, int minimumQuantity, int maximumQuantity, boolean required, String notes, List<String> alternatives)
	{
		this.itemOrCategory = itemOrCategory;
		this.quantityMode = quantityMode;
		this.minimumQuantity = minimumQuantity;
		this.maximumQuantity = maximumQuantity;
		this.required = required;
		this.notes = notes == null ? "" : notes;
		this.alternatives = Collections.unmodifiableList(alternatives);
	}

	public String getItemOrCategory()
	{
		return itemOrCategory;
	}

	public QuantityMode getQuantityMode()
	{
		return quantityMode;
	}

	public int getMinimumQuantity()
	{
		return minimumQuantity;
	}

	public int getMaximumQuantity()
	{
		return maximumQuantity;
	}

	public boolean isRequired()
	{
		return required;
	}

	public String getNotes()
	{
		return notes;
	}

	public List<String> getAlternatives()
	{
		return alternatives;
	}
}
