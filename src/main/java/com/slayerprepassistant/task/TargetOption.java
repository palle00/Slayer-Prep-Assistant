package com.slayerprepassistant.task;

public class TargetOption
{
	private final String displayName;
	private final String npcName;
	private final String wikiPage;
	private final String strategyPage;
	private final Integer combatLevel;
	private final Integer slayerLevel;
	private final String shortDescription;
	private final boolean hasStrategyGuide;

	public TargetOption(String displayName, String npcName, String wikiPage, String strategyPage, Integer combatLevel, Integer slayerLevel, String shortDescription, boolean hasStrategyGuide)
	{
		this.displayName = displayName;
		this.npcName = npcName;
		this.wikiPage = wikiPage;
		this.strategyPage = strategyPage;
		this.combatLevel = combatLevel;
		this.slayerLevel = slayerLevel;
		this.shortDescription = shortDescription;
		this.hasStrategyGuide = hasStrategyGuide;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public String getNpcName()
	{
		return npcName;
	}

	public String getWikiPage()
	{
		return wikiPage;
	}

	public String getStrategyPage()
	{
		return strategyPage;
	}

	public Integer getCombatLevel()
	{
		return combatLevel;
	}

	public Integer getSlayerLevel()
	{
		return slayerLevel;
	}

	public String getShortDescription()
	{
		return shortDescription;
	}

	public boolean isHasStrategyGuide()
	{
		return hasStrategyGuide;
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
