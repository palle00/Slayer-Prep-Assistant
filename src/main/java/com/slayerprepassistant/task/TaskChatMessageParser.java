package com.slayerprepassistant.task;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.client.util.Text;

public class TaskChatMessageParser
{
	private static final Pattern TASK_STATUS = Pattern.compile("(?i).*You have a Slayer task:\\s*([^\\.]+).*");
	private static final Pattern REMAINING = Pattern.compile("(?i).*You have\\s+(\\d+)\\s+(.+?)\\s+remaining\\.?$");
	private static final Pattern ASSIGNED = Pattern.compile("(?i).*(?:You're|You are) assigned to kill\\s+(?:(\\d+)\\s+)?(.+?)(?:\\.|;|$).*");
	private static final Pattern NEW_TASK = Pattern.compile("(?i).*Your new task is to kill\\s+(\\d+)\\s+(.+?)(?:\\.|;|$).*");
	private static final Pattern COMPLETE = Pattern.compile("(?i).*(?:completed your task|task is complete|no longer have a slayer task).*");

	public Optional<SlayerTaskContext> parse(String message, SlayerTaskContext previous)
	{
		String clean = Text.removeTags(message == null ? "" : message).replace('\u00a0', ' ').trim();
		if (clean.isEmpty())
		{
			return Optional.empty();
		}
		if (COMPLETE.matcher(clean).matches())
		{
			return Optional.of(SlayerTaskContext.none());
		}
		Matcher remaining = REMAINING.matcher(clean);
		if (remaining.matches())
		{
			int amount = parseAmount(remaining.group(1));
			String taskName = cleanTaskName(remaining.group(2));
			return Optional.of(new SlayerTaskContext(taskName, amount, previousInitial(previous, taskName, amount), previousLocation(previous, taskName), true));
		}
		Matcher newTask = NEW_TASK.matcher(clean);
		if (newTask.matches())
		{
			int amount = parseAmount(newTask.group(1));
			String taskName = cleanTaskName(newTask.group(2));
			return Optional.of(new SlayerTaskContext(taskName, amount, amount, "", true));
		}
		Matcher assigned = ASSIGNED.matcher(clean);
		if (assigned.matches())
		{
			int amount = parseAmount(assigned.group(1));
			String taskName = cleanTaskName(assigned.group(2));
			return Optional.of(new SlayerTaskContext(taskName, amount, amount, "", true));
		}
		Matcher taskStatus = TASK_STATUS.matcher(clean);
		if (taskStatus.matches())
		{
			String taskName = cleanTaskName(taskStatus.group(1));
			return Optional.of(new SlayerTaskContext(taskName, previousRemaining(previous, taskName), previousInitial(previous, taskName, previousRemaining(previous, taskName)), previousLocation(previous, taskName), true));
		}
		return Optional.empty();
	}

	private int previousRemaining(SlayerTaskContext previous, String taskName)
	{
		return previous != null && previous.isActive() && previous.getTaskName().equalsIgnoreCase(taskName) ? previous.getRemainingAmount() : 0;
	}

	private int previousInitial(SlayerTaskContext previous, String taskName, int fallback)
	{
		return previous != null && previous.isActive() && previous.getTaskName().equalsIgnoreCase(taskName) ? previous.getInitialAmount() : fallback;
	}

	private String previousLocation(SlayerTaskContext previous, String taskName)
	{
		return previous != null && previous.isActive() && previous.getTaskName().equalsIgnoreCase(taskName) ? previous.getAssignedLocation() : "";
	}

	private int parseAmount(String value)
	{
		if (value == null || value.trim().isEmpty())
		{
			return 0;
		}
		try
		{
			return Integer.parseInt(value);
		}
		catch (NumberFormatException ex)
		{
			return 0;
		}
	}

	private String cleanTaskName(String value)
	{
		String cleaned = value == null ? "" : value.trim();
		cleaned = cleaned.replaceFirst("(?i)\\s+in\\s+.+$", "");
		return cleaned.replaceAll("\\s+", " ");
	}
}
