package com.slayerprepassistant.task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TaskChatMessageParserTest
{
	private final TaskChatMessageParser parser = new TaskChatMessageParser();

	@Test
	public void parsesRemainingTaskMessage()
	{
		SlayerTaskContext context = parser.parse("You have 148 Abyssal Demons remaining.", SlayerTaskContext.none()).get();

		assertTrue(context.isActive());
		assertEquals("Abyssal Demons", context.getTaskName());
		assertEquals(148, context.getRemainingAmount());
	}

	@Test
	public void parsesTaskStatusMessageWithoutCount()
	{
		SlayerTaskContext context = parser.parse("You have a Slayer task: Abyssal Demons.", SlayerTaskContext.none()).get();

		assertTrue(context.isActive());
		assertEquals("Abyssal Demons", context.getTaskName());
		assertEquals(0, context.getRemainingAmount());
	}

	@Test
	public void parsesCompletionMessage()
	{
		SlayerTaskContext previous = new SlayerTaskContext("Abyssal demons", 1, 150, "", true);
		SlayerTaskContext context = parser.parse("You have completed your task!", previous).get();

		assertFalse(context.isActive());
	}
}
