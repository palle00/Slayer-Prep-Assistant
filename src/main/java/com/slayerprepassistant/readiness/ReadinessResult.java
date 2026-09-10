package com.slayerprepassistant.readiness;

import java.util.ArrayList;
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
		this.percentage = Math.max(0, Math.min(100, percentage));
		this.criticalBlockers = criticalBlockers == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(criticalBlockers));
		this.warnings = warnings == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(warnings));
		this.readyItems = readyItems == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(readyItems));
		this.unknownItems = unknownItems == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(unknownItems));
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