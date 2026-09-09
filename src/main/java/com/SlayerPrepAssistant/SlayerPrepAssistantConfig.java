package com.SlayerPrepAssistant;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("slayerprepassistant")
public interface SlayerPrepAssistantConfig extends Config
{
	@ConfigItem(
			keyName = "greeting",
			name = "Welcome Greeting",
			description = "The message to show to the user when they login"
	)
	default String greeting()
	{
		return "Hello";
	}
}