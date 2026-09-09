package com.slayerprepassistant.readiness;

import java.util.Collections;
import java.util.List;

public class ReadinessResult
{
	private final int percentage;
	private final List<ReadinessIssue> criticalBlockers;
	private final List<ReadinessIssue> warnings;
	private final List<String> readyItems;
	private final List<String> unknownItems;

	public ReadinessResult(int percentage, List<ReadinessIssue> criticalBlockers, List<ReadinessIssue> warnings, List<String> readyItems, List<String> unknownItems)
	{
		this.percentage = percentage;
		this.criticalBlockers = Collections.unmodifiableList(criticalBlockers);
		this.warnings = Collections.unmodifiableList(warnings);
		this.readyItems = Collections.unmodifiableList(readyItems);
		this.unknownItems = Collections.unmodifiableList(unknownItems);
	}

	public int getPercentage()
	{
		return percentage;
	}

	public List<ReadinessIssue> getCriticalBlockers()
	{
		return criticalBlockers;
	}

	public List<ReadinessIssue> getWarnings()
	{
		return warnings;
	}

	public List<String> getReadyItems()
	{
		return readyItems;
	}

	public List<String> getUnknownItems()
	{
		return unknownItems;
	}
}
