package com.slayerprepassistant.task;

public class TargetOption
{
	private final String displayName;
	private final String wikiPage;
	private final String strategyPage;

	public TargetOption(String displayName, String wikiPage, String strategyPage)
	{
		this.displayName = displayName;
		this.wikiPage = wikiPage;
		this.strategyPage = strategyPage;
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

	public String lookupKey()
	{
		return lookupKey(this);
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
		return java.util.Objects.equals(displayName, that.displayName)
			&& java.util.Objects.equals(strategyPage, that.strategyPage);
	}

	@Override
	public int hashCode()
	{
		return java.util.Objects.hash(displayName, strategyPage);
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
