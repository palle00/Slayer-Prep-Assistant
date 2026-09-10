package com.slayerprepassistant.task;

import java.util.Objects;

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
	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		if (!(other instanceof SlayerTaskContext))
		{
			return false;
		}
		SlayerTaskContext that = (SlayerTaskContext) other;
		return remainingAmount == that.remainingAmount
			&& initialAmount == that.initialAmount
			&& active == that.active
			&& Objects.equals(taskName, that.taskName)
			&& Objects.equals(assignedLocation, that.assignedLocation);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(taskName, remainingAmount, initialAmount, assignedLocation, active);
	}

}