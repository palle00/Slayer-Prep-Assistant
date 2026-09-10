package com.slayerprepassistant.wiki;

import com.slayerprepassistant.task.TargetOption;

public class MonsterVariant
{
	private final String displayName;
	private final String wikiPageTitle;
	private final String wikiUrl;

	public MonsterVariant(String displayName, String wikiPageTitle, String wikiUrl)
	{
		this.displayName = displayName == null ? "" : displayName;
		this.wikiPageTitle = wikiPageTitle == null ? "" : wikiPageTitle;
		this.wikiUrl = wikiUrl == null ? "" : wikiUrl;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public String getWikiPageTitle()
	{
		return wikiPageTitle;
	}

	public String getWikiUrl()
	{
		return wikiUrl;
	}

	public TargetOption toTargetOption()
	{
		return new TargetOption(displayName, wikiPageTitle, "Strategies/" + wikiPageTitle);
	}
}
