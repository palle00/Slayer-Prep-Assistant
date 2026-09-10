package com.slayerprepassistant.task;

public class SlayerTaskContext
{
	private static final SlayerTaskContext NONE = new SlayerTaskContext("", 0, 0, "", false);

	private final String taskName;
	private final int remainingAmount;
	private final int initialAmount;
	private final String assignedLocation;
	private final boolean active;

	public SlayerTaskContext(String taskName, int remainingAmount, int initialAmount, String assignedLocation, boolean active)
	{
		this.taskName = taskName == null ? "" : taskName;
		this.remainingAmount = Math.max(0, remainingAmount);
		this.initialAmount = Math.max(0, initialAmount);
		this.assignedLocation = assignedLocation == null ? "" : assignedLocation;
		this.active = active;
	}

	public static SlayerTaskContext none()
	{
		return NONE;
	}

	public String getTaskName()
	{
		return taskName;
	}

	public int getRemainingAmount()
	{
		return remainingAmount;
	}

	public int getInitialAmount()
	{
		return initialAmount;
	}

	public String getAssignedLocation()
	{
		return assignedLocation;
	}

	public boolean isActive()
	{
		return active;
	}
}