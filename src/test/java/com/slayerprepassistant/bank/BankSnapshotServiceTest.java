package com.slayerprepassistant.bank;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Node;
import org.junit.Test;

public class BankSnapshotServiceTest
{
	@Test
	public void bankQuantitiesIgnorePlaceholderItemIds()
	{
		ItemContainer container = new TestItemContainer(
			new Item(100, 1),
			new Item(200, 1));

		HashSet<Integer> ignoredIds = new HashSet<>(Collections.singletonList(100));

		assertFalse(BankSnapshotService.toQuantities(container, ignoredIds).containsKey(100));
		assertTrue(BankSnapshotService.toQuantities(container, ignoredIds).containsKey(200));
	}

	private static final class TestItemContainer implements ItemContainer
	{
		private final Item[] items;

		private TestItemContainer(Item... items)
		{
			this.items = items;
		}

		@Override
		public int getId()
		{
			return 0;
		}

		@Override
		public Item[] getItems()
		{
			return items;
		}

		@Override
		public Item getItem(int index)
		{
			return items[index];
		}

		@Override
		public boolean contains(int itemId)
		{
			for (Item item : items)
			{
				if (item.getId() == itemId)
				{
					return true;
				}
			}
			return false;
		}

		@Override
		public int count(int itemId)
		{
			int count = 0;
			for (Item item : items)
			{
				if (item.getId() == itemId)
				{
					count += item.getQuantity();
				}
			}
			return count;
		}

		@Override
		public int size()
		{
			return items.length;
		}

		@Override
		public int count()
		{
			return items.length;
		}

		@Override
		public int find(int itemId)
		{
			for (int i = 0; i < items.length; i++)
			{
				if (items[i].getId() == itemId)
				{
					return i;
				}
			}
			return -1;
		}

		@Override
		public Node getNext()
		{
			return null;
		}

		@Override
		public Node getPrevious()
		{
			return null;
		}

		@Override
		public long getHash()
		{
			return 0;
		}
	}
}
