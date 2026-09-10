package com.slayerprepassistant.ui;

import com.slayerprepassistant.bank.OwnershipState;
import com.slayerprepassistant.gear.RecommendedItem;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import javax.swing.JComponent;

class InventorySlotTile extends JComponent
{
	interface WikiItemImageLoader
	{
		void load(RecommendedItem item, int size, Consumer<BufferedImage> callback);
	}

	private final RecommendedItem item;
	private final int quantity;
	private final OwnershipState state;
	private final WikiItemImageLoader wikiItemImageLoader;
	private Image image;

	InventorySlotTile(
		RecommendedItem item,
		int quantity,
		OwnershipState state,
		BiFunction<RecommendedItem, Integer, Image> itemImageProvider,
		WikiItemImageLoader wikiItemImageLoader)
	{
		this.item = item;
		this.quantity = quantity;
		this.state = state;
		this.wikiItemImageLoader = wikiItemImageLoader;
		this.image = itemImageProvider.apply(item, 28);
		setPreferredSize(new Dimension(42, 42));
		setMinimumSize(new Dimension(42, 42));
		if (this.image == null && this.wikiItemImageLoader != null)
		{
			this.wikiItemImageLoader.load(item, 28, image ->
			{
				this.image = image;
				repaint();
			});
		}
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		super.paintComponent(graphics);
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		int width = getWidth();
		int height = getHeight();
		boolean empty = item == null || item.getName() == null || item.getName().trim().isEmpty();
		Color borderColor = empty ? SlayerPrepAssistantPanel.BORDER : statusColor(state);

		g.setColor(new Color(18, 18, 18));
		g.fillRoundRect(0, 0, width, height, SlayerPrepAssistantPanel.CONTROL_RADIUS, SlayerPrepAssistantPanel.CONTROL_RADIUS);
		g.setColor(empty ? new Color(24, 24, 24) : new Color(31, 31, 31));
		g.fillRoundRect(1, 1, width - 2, height - 2, SlayerPrepAssistantPanel.CONTROL_RADIUS, SlayerPrepAssistantPanel.CONTROL_RADIUS);
		g.setColor(borderColor);
		g.drawRoundRect(0, 0, width - 1, height - 1, SlayerPrepAssistantPanel.CONTROL_RADIUS, SlayerPrepAssistantPanel.CONTROL_RADIUS);

		if (!empty)
		{
			g.fillRect(1, height - 4, width - 2, 3);
			drawItem(g, width, height);
			drawQuantity(g, width);
		}
		g.dispose();
	}

	private void drawItem(Graphics2D g, int width, int height)
	{
		if (image != null)
		{
			int x = (width - image.getWidth(null)) / 2;
			int y = (height - image.getHeight(null)) / 2 - 1;
			g.drawImage(image, x, y, null);
			return;
		}
		String fallback = item.getName().substring(0, 1).toUpperCase(java.util.Locale.ROOT);
		g.setFont(getFont().deriveFont(Font.BOLD, SlayerPrepAssistantPanel.FONT_SM));
		g.setColor(SlayerPrepAssistantPanel.MUTED);
		int textWidth = g.getFontMetrics().stringWidth(fallback);
		g.drawString(fallback, (width - textWidth) / 2, height / 2 + 5);
	}

	private void drawQuantity(Graphics2D g, int width)
	{
		if (quantity <= 0)
		{
			return;
		}
		String text = String.valueOf(quantity);
		g.setFont(getFont().deriveFont(Font.BOLD, SlayerPrepAssistantPanel.FONT_XS));
		int textWidth = g.getFontMetrics().stringWidth(text);
		int badgeWidth = Math.max(13, textWidth + 6);
		g.setColor(new Color(15, 15, 15));
		g.fillRect(width - badgeWidth - 2, 2, badgeWidth, 14);
		g.setColor(SlayerPrepAssistantPanel.GOLD);
		g.drawRect(width - badgeWidth - 2, 2, badgeWidth, 14);
		g.drawString(text, width - badgeWidth + 1, 13);
	}

	private Color statusColor(OwnershipState state)
	{
		switch (state)
		{
			case EQUIPPED:
				return SlayerPrepAssistantPanel.GREEN;
			case OWNED_IN_INVENTORY:
				return SlayerPrepAssistantPanel.BLUE;
			case OWNED_IN_BANK:
				return SlayerPrepAssistantPanel.YELLOW;
			case MISSING:
				return SlayerPrepAssistantPanel.RED;
			case UNKNOWN:
			default:
				return SlayerPrepAssistantPanel.MUTED;
		}
	}
}
