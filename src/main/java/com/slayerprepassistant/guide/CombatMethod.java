package com.slayerprepassistant.guide;

public enum CombatMethod
{
	GENERAL,
	MELEE,
	RANGED,
	MAGIC;

	@Override
	public String toString()
	{
		String lower = name().toLowerCase(java.util.Locale.ROOT);
		return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
	}
}
