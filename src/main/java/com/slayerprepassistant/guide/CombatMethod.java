package com.slayerprepassistant.guide;

public enum CombatMethod
{
	MELEE,
	RANGED,
	MAGIC;

	public static CombatMethod defaultMethod()
	{
		return MELEE;
	}

	@Override
	public String toString()
	{
		String lower = name().toLowerCase(java.util.Locale.ROOT);
		return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
	}
}
