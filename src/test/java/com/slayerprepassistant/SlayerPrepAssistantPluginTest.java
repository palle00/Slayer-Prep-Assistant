package com.slayerprepassistant;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SlayerPrepAssistantPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SlayerPrepAssistantPlugin.class);
		RuneLite.main(args);
	}
}
