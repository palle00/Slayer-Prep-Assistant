package com.slayerprepassistant.wiki;

import com.slayerprepassistant.gear.GearSlot;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class WikiGearSlots
{
	private static final Pattern SLOT_RANK = Pattern.compile("^\\|?\\s*[A-Za-z ]+(\\d+)\\s*$");

	private WikiGearSlots()
	{
	}

	static GearSlot parse(String value)
	{
		String normalized = value.trim()
			.replaceFirst("^\\|", "")
			.replaceFirst("\\d+$", "")
			.trim()
			.toUpperCase(Locale.ROOT)
			.replace(' ', '_');
		if ("TORSO".equals(normalized))
		{
			normalized = "BODY";
		}
		if ("AMMUNITION".equals(normalized))
		{
			normalized = "AMMO";
		}
		if ("AMMO2".equals(normalized))
		{
			normalized = "AMMO";
		}
		if ("GLOVES".equals(normalized))
		{
			normalized = "HANDS";
		}
		if ("BOOTS".equals(normalized))
		{
			normalized = "FEET";
		}
		try
		{
			return GearSlot.valueOf(normalized);
		}
		catch (IllegalArgumentException ex)
		{
			return null;
		}
	}

	static Integer templateRank(String slotKey)
	{
		Matcher matcher = SLOT_RANK.matcher(slotKey == null ? "" : slotKey.trim());
		return matcher.matches() ? Integer.parseInt(matcher.group(1)) : null;
	}
}
