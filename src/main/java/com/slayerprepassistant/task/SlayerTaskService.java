package com.slayerprepassistant.task;

import java.util.Optional;
import net.runelite.api.Client;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;

public class SlayerTaskService
{
	private static final int BOSS_TASK_TARGET_ID = 98;

	private final TaskChatMessageParser chatMessageParser = new TaskChatMessageParser();
	private SlayerTaskContext chatDetectedTask = SlayerTaskContext.none();

	public SlayerTaskService(ConfigManager configManager)
	{
	}

	public SlayerTaskContext getCurrentTask()
	{
		return getCurrentTask(null);
	}

	public SlayerTaskContext getCurrentTask(Client client)
	{
		if (client != null)
		{
			return readClientTask(client);
		}
		return chatDetectedTask;
	}

	public boolean applyChatMessage(String message)
	{
		Optional<SlayerTaskContext> parsed = chatMessageParser.parse(message, chatDetectedTask);
		if (!parsed.isPresent())
		{
			return false;
		}
		chatDetectedTask = parsed.get();
		return true;
	}

	private SlayerTaskContext readClientTask(Client client)
	{
		int remaining = client.getVarpValue(VarPlayerID.SLAYER_COUNT);
		int target = client.getVarpValue(VarPlayerID.SLAYER_TARGET);
		if (remaining <= 0 || target <= 0)
		{
			return SlayerTaskContext.none();
		}
		try
		{
			Integer taskRow = resolveTaskRow(client, target);
			if (taskRow == null)
			{
				return SlayerTaskContext.none();
			}
			Object[] names = client.getDBTableField(taskRow, DBTableID.SlayerTask.COL_NAME_UPPERCASE, 0);
			if (names.length == 0 || !(names[0] instanceof String))
			{
				return SlayerTaskContext.none();
			}
			String taskName = (String) names[0];
			String location = resolveTaskLocation(client);
			int initialAmount = readInitialAmount(client);
			return new SlayerTaskContext(taskName, remaining, initialAmount, location, true);
		}
		catch (RuntimeException ex)
		{
			return SlayerTaskContext.none();
		}
	}

	private Integer resolveTaskRow(Client client, int target)
	{
		if (target == BOSS_TASK_TARGET_ID)
		{
			int bossId = client.getVarbitValue(VarbitID.SLAYER_TARGET_BOSSID);
			java.util.List<Integer> sublistRows = client.getDBRowsByValue(DBTableID.SlayerTaskSublist.ID, DBTableID.SlayerTaskSublist.COL_TASK_SUBTABLE_ID, 0, bossId);
			if (sublistRows.isEmpty())
			{
				return null;
			}
			Object[] taskIds = client.getDBTableField(sublistRows.get(0), DBTableID.SlayerTaskSublist.COL_TASK, 0);
			if (taskIds.length == 0 || !(taskIds[0] instanceof Integer))
			{
				return null;
			}
			return (Integer) taskIds[0];
		}
		java.util.List<Integer> rows = client.getDBRowsByValue(DBTableID.SlayerTask.ID, DBTableID.SlayerTask.COL_ID, 0, target);
		return rows.isEmpty() ? null : rows.get(0);
	}

	private String resolveTaskLocation(Client client)
	{
		int area = client.getVarpValue(VarPlayerID.SLAYER_AREA);
		if (area <= 0)
		{
			return "";
		}
		java.util.List<Integer> rows = client.getDBRowsByValue(DBTableID.SlayerArea.ID, DBTableID.SlayerArea.COL_AREA_ID, 0, area);
		if (rows.isEmpty())
		{
			return "";
		}
		Object[] names = client.getDBTableField(rows.get(0), DBTableID.SlayerArea.COL_AREA_NAME_IN_HELPER, 0);
		return names.length > 0 && names[0] instanceof String ? (String) names[0] : "";
	}

	private int readInitialAmount(Client client)
	{
		int initialAmount = client.getVarpValue(VarPlayerID.SLAYER_COUNT_ORIGINAL);
		if (client.getVarbitValue(VarbitID.SLAYER_MODIFIER_ID) == 2)
		{
			int modifier = client.getVarbitValue(VarbitID.SLAYER_MODIFIER_VALUE);
			if (client.getVarbitValue(VarbitID.SLAYER_MODIFIER_NEGATIVE) == 1)
			{
				modifier = -modifier;
			}
			initialAmount += modifier;
		}
		return initialAmount;
	}

}
