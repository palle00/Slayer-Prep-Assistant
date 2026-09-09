package com.slayerprepassistant;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("slayer-prep-assistant")
public interface SlayerPrepAssistantConfig extends Config
{
	@ConfigItem(
		keyName = "thirdPartyWarningAcknowledged",
		name = "",
		description = "",
		hidden = true
	)
	default boolean thirdPartyWarningAcknowledged()
	{
		return false;
	}

	@ConfigItem(
		keyName = "useWikiStrategyData",
		name = "Use Wiki strategy data",
		description = "Retrieve public strategy guides from the Old School RuneScape Wiki",
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers"
	)
	default boolean useWikiStrategyData()
	{
		return false;
	}

	@ConfigItem(
		keyName = "useWikiPriceData",
		name = "Use Wiki price data",
		description = "Retrieve public item price data from the Old School RuneScape Wiki",
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers"
	)
	default boolean useWikiPriceData()
	{
		return false;
	}
}
