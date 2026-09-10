package com.slayerprepassistant.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.function.IntSupplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

class SkeletonSection extends RoundedPanel
{
	private final IntSupplier frameSupplier;

	SkeletonSection(String title, int rows, boolean equipment, IntSupplier frameSupplier)
	{
		super(new BorderLayout(0, 6), SlayerPrepAssistantPanel.PANEL, SlayerPrepAssistantPanel.CARD_RADIUS);
		this.frameSupplier = frameSupplier == null ? () -> 0 : frameSupplier;

		setBorder(BorderFactory.createCompoundBorder(
				new RoundedBorder(SlayerPrepAssistantPanel.BORDER, SlayerPrepAssistantPanel.CARD_RADIUS),
				BorderFactory.createEmptyBorder(7, 8, 7, 8)));
		setAlignmentX(Component.LEFT_ALIGNMENT);

		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(SlayerPrepAssistantPanel.PANEL);
		header.add(label(title), BorderLayout.WEST);
		add(header, BorderLayout.NORTH);

		int effectiveRows = Math.max(0, rows);
		JPanel body = equipment ? skeletonEquipmentRows(effectiveRows) : skeletonInventoryRows(effectiveRows);
		add(body, BorderLayout.CENTER);
		fitHeight(this);
	}

	private JPanel skeletonEquipmentRows(int rows)
	{
		JPanel panel = verticalPanel(SlayerPrepAssistantPanel.PANEL);
		for (int i = 0; i < rows; i++)
		{
			JPanel row = new JPanel(new BorderLayout(7, 0));
			row.setBackground(i % 2 == 0 ? new Color(29, 29, 29) : new Color(24, 24, 24));
			row.setBorder(BorderFactory.createEmptyBorder(4, 3, 4, 0));
			row.add(new SkeletonBlock(30, 30, frameSupplier), BorderLayout.WEST);

			JPanel text = verticalPanel(row.getBackground());
			text.add(new SkeletonBlock(56, 8, frameSupplier));
			text.add(Box.createVerticalStrut(4));
			text.add(new SkeletonBlock(i % 3 == 0 ? 96 : 120, 10, frameSupplier));
			row.add(text, BorderLayout.CENTER);
			row.add(new SkeletonBlock(42, 10, frameSupplier), BorderLayout.EAST);
			panel.add(row);
		}
		return panel;
	}

	private JPanel skeletonInventoryRows(int rows)
	{
		JPanel panel = verticalPanel(SlayerPrepAssistantPanel.PANEL);
		JPanel grid = new JPanel(new GridLayout(0, 4, 5, 5));
		grid.setBackground(SlayerPrepAssistantPanel.PANEL);
		for (int i = 0; i < rows * 4; i++)
		{
			grid.add(new SkeletonBlock(42, 42, frameSupplier));
		}
		panel.add(grid);
		return panel;
	}

	private JPanel verticalPanel(Color background)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(background == null ? SlayerPrepAssistantPanel.PANEL : background);
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		return panel;
	}

	private JLabel label(String text)
	{
		JLabel label = new JLabel(text == null ? "" : text);
		label.setForeground(SlayerPrepAssistantPanel.GOLD);
		Font baseFont = label.getFont();
		if (baseFont != null)
		{
			label.setFont(baseFont.deriveFont(Font.BOLD, SlayerPrepAssistantPanel.FONT_XS));
		}
		return label;
	}

	private void fitHeight(Component component)
	{
		if (component == null)
		{
			return;
		}
		Dimension preferred = component.getPreferredSize();
		int prefHeight = preferred != null ? preferred.height : 50;
		component.setMaximumSize(new Dimension(Integer.MAX_VALUE, prefHeight));
	}

	private static class SkeletonBlock extends JComponent
	{
		private final int radius = SlayerPrepAssistantPanel.CONTROL_RADIUS;
		private final IntSupplier frameSupplier;

		SkeletonBlock(int preferredWidth, int preferredHeight, IntSupplier frameSupplier)
		{
			this.frameSupplier = frameSupplier == null ? () -> 0 : frameSupplier;
			Dimension dim = new Dimension(Math.max(1, preferredWidth), Math.max(1, preferredHeight));
			setPreferredSize(dim);
			setMinimumSize(dim);
			setMaximumSize(dim);
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			super.paintComponent(graphics);
			if (graphics == null)
			{
				return;
			}
			Graphics2D g = (Graphics2D) graphics.create();
			if (g == null)
			{
				return;
			}
			try
			{
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				int width = getWidth();
				int height = getHeight();
				if (width <= 0 || height <= 0)
				{
					return;
				}

				int frame = frameSupplier.getAsInt();
				int pulse = Math.abs(6 - frame);
				int shade = 30 + pulse * 3;
				g.setColor(new Color(shade, shade, shade));
				g.fillRoundRect(0, 0, width, height, radius, radius);

				g.setColor(new Color(58, 58, 58, 80));
				int shimmerX = (frame * (width + 16) / 12) - 16;
				g.fillRoundRect(shimmerX, 0, Math.max(8, width / 3), height, radius, radius);
			}
			finally
			{
				g.dispose();
			}
		}
	}
}
