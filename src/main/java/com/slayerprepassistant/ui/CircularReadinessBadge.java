package com.slayerprepassistant.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

class CircularReadinessBadge extends JComponent
{
	private int percent;
	private boolean visibleValue;

	CircularReadinessBadge()
	{
		setPreferredSize(new Dimension(58, 58));
		setMinimumSize(new Dimension(58, 58));
		setMaximumSize(new Dimension(58, 58));
	}

	void setReadiness(int percent, boolean visibleValue)
	{
		this.percent = Math.max(0, Math.min(100, percent));
		this.visibleValue = visibleValue;
		repaint();
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
			int size = Math.min(getWidth(), getHeight()) - 8;
			if (size <= 0)
			{
				return;
			}
			int x = (getWidth() - size) / 2;
			int y = (getHeight() - size) / 2;
			g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(new Color(50, 50, 50));
			g.drawOval(x, y, size, size);

			Color color = percent < 70 ? SlayerPrepAssistantPanel.RED : percent < 90 ? SlayerPrepAssistantPanel.YELLOW : SlayerPrepAssistantPanel.GREEN;
			g.setColor(visibleValue ? color : SlayerPrepAssistantPanel.MUTED);
			g.drawArc(x, y, size, size, 90, -Math.round(360f * percent / 100f));

			Font baseFont = getFont();
			if (baseFont != null)
			{
				g.setFont(baseFont.deriveFont(Font.BOLD, SlayerPrepAssistantPanel.FONT_XL));
			}
			String percentText = visibleValue ? percent + "%" : "--";
			int textWidth = g.getFontMetrics().stringWidth(percentText);
			g.drawString(percentText, (getWidth() - textWidth) / 2, getHeight() / 2 - 1);

			if (baseFont != null)
			{
				g.setFont(baseFont.deriveFont(Font.BOLD, SlayerPrepAssistantPanel.FONT_XS));
			}
			String label = visibleValue ? "Ready" : "";
			int labelWidth = g.getFontMetrics().stringWidth(label);
			g.drawString(label, (getWidth() - labelWidth) / 2, getHeight() / 2 + 13);
		}
		finally
		{
			g.dispose();
		}
	}
}