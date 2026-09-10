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
		String name = name();
		return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
	}
}