package com.slayerprepassistant.gear;

import java.util.Locale;

public enum GearSlot
{
	HEAD,
	CAPE,
	NECK,
	AMMO,
	WEAPON,
	BODY,
	SHIELD,
	LEGS,
	HANDS,
	FEET,
	RING;

	public String displayName()
	{
		String lower = name().toLowerCase(Locale.ROOT);
		return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
	}
}
