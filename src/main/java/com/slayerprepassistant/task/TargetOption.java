package com.slayerprepassistant.task;

import java.util.Objects;

public class TargetOption
{
	private final String displayName;
	private final String wikiPage;
	private final String strategyPage;

	public TargetOption(String displayName, String wikiPage, String strategyPage)
	{
		this.displayName = displayName == null ? "" : displayName;
		this.wikiPage = wikiPage == null ? "" : wikiPage;
		this.strategyPage = strategyPage == null ? "" : strategyPage;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public String getWikiPage()
	{
		return wikiPage;
	}

	public String getStrategyPage()
	{
		return strategyPage;
	}


	public static String lookupKey(TargetOption target)
	{
		return target == null ? "" : target.displayName + "|" + target.wikiPage + "|" + target.strategyPage;
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		if (!(other instanceof TargetOption))
		{
			return false;
		}
		TargetOption that = (TargetOption) other;
		return Objects.equals(displayName, that.displayName)
				&& Objects.equals(strategyPage, that.strategyPage);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(displayName, strategyPage);
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}