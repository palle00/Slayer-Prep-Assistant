package com.slayerprepassistant.gear;

public enum LoadoutMode
{
	BEST_I_OWN("Best I Own"),
	MAX("Max");

	private final String displayName;

	LoadoutMode(String displayName)
	{
		this.displayName = displayName;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}