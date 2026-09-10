package com.slayerprepassistant.task;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class SlayerTaskServiceTest
{
	@Test
	public void startsWithNoTaskWhenNoClientOrChatDataExists()
	{
		SlayerTaskService service = new SlayerTaskService();

		assertFalse(service.getCurrentTask(null).isActive());
	}
}
