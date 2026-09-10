package com.slayerprepassistant.guide;

public class InventoryRecommendation
{
	private final String itemOrCategory;
	private final int minimumQuantity;
	private final boolean required;

	public InventoryRecommendation(String itemOrCategory, int minimumQuantity, boolean required)
	{
		this.itemOrCategory = itemOrCategory == null ? "" : itemOrCategory;
		this.minimumQuantity = Math.max(0, minimumQuantity);
		this.required = required;
	}

	public String getItemOrCategory()
	{
		return itemOrCategory;
	}

	public int getMinimumQuantity()
	{
		return minimumQuantity;
	}

	public boolean isRequired()
	{
		return required;
	}
}