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
		this.frameSupplier = frameSupplier;
		setBorder(BorderFactory.createCompoundBorder(
			new RoundedBorder(SlayerPrepAssistantPanel.BORDER, SlayerPrepAssistantPanel.CARD_RADIUS),
			BorderFactory.createEmptyBorder(7, 8, 7, 8)));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		JPanel header = new JPanel(new BorderLayout());
		header.setBackground(SlayerPrepAssistantPanel.PANEL);
		header.add(label(title), BorderLayout.WEST);
		add(header, BorderLayout.NORTH);

		JPanel body = equipment ? skeletonEquipmentRows(rows) : skeletonInventoryRows(rows);
		add(body, BorderLayout.CENTER);
		fitHeight(this);
	}

	private JPanel skeletonEquipmentRows(int rows)
	{
		JPanel panel = verticalPanel(SlayerPrepAssistantPanel.PANEL);
		for (int i = 0; i < rows; i++)
		{
			JPanel row = new JPanel(new BorderLayout(7, 0));
			row.setBackground(i % 2 == 0 ? new Color(32, 32, 32) : new Color(27, 27, 27));
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
		panel.setBackground(background);
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		return panel;
	}

	private JLabel label(String text)
	{
		JLabel label = new JLabel(text == null ? "" : text);
		label.setForeground(SlayerPrepAssistantPanel.GOLD);
		label.setFont(label.getFont().deriveFont(Font.BOLD, SlayerPrepAssistantPanel.FONT_XS));
		return label;
	}

	private void fitHeight(Component component)
	{
		Dimension preferred = component.getPreferredSize();
		component.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferred.height));
	}

	private static class SkeletonBlock extends JComponent
	{
		private final int radius = SlayerPrepAssistantPanel.CONTROL_RADIUS;
		private final IntSupplier frameSupplier;

		SkeletonBlock(int preferredWidth, int preferredHeight, IntSupplier frameSupplier)
		{
			this.frameSupplier = frameSupplier;
			setPreferredSize(new Dimension(preferredWidth, preferredHeight));
			setMinimumSize(new Dimension(preferredWidth, preferredHeight));
			setMaximumSize(new Dimension(preferredWidth, preferredHeight));
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int frame = frameSupplier.getAsInt();
			int pulse = Math.abs(6 - frame);
			int shade = 43 + pulse * 4;
			g.setColor(new Color(shade, shade, shade));
			g.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
			g.setColor(new Color(74, 74, 74, 80));
			int shimmerX = (frame * (getWidth() + 16) / 12) - 16;
			g.fillRoundRect(shimmerX, 0, Math.max(8, getWidth() / 3), getHeight(), radius, radius);
			g.dispose();
		}
	}
}
