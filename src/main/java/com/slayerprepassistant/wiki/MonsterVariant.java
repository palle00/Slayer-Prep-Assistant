package com.slayerprepassistant.wiki;

import com.slayerprepassistant.task.TargetOption;
import java.util.Objects;

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


	public TargetOption toTargetOption()
	{
		return new TargetOption(displayName, wikiPageTitle, "Strategies/" + wikiPageTitle);
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MonsterVariant that = (MonsterVariant) o;
		return displayName.equals(that.displayName) &&
				wikiPageTitle.equals(that.wikiPageTitle) &&
				wikiUrl.equals(that.wikiUrl);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(displayName, wikiPageTitle, wikiUrl);
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}