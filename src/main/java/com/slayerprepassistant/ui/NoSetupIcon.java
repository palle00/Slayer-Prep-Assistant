package com.slayerprepassistant.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

class NoSetupIcon extends JComponent
{
	NoSetupIcon()
	{
		setPreferredSize(new Dimension(62, 50));
		setMinimumSize(new Dimension(62, 50));
		setMaximumSize(new Dimension(62, 50));
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
			int centerX = width / 2;

			g.setColor(new Color(35, 35, 35));
			g.fillOval(centerX - 20, 6, 40, 40);
			g.setColor(new Color(87, 87, 87));
			g.drawOval(centerX - 20, 6, 40, 40);

			g.setColor(new Color(194, 194, 194));
			g.fillOval(centerX - 15, 12, 30, 26);
			g.fillRoundRect(centerX - 10, 30, 20, 11, 7, 7);

			g.setColor(new Color(70, 70, 70));
			g.fillOval(centerX - 10, 22, 8, 8);
			g.fillOval(centerX + 2, 22, 8, 8);
			g.fillOval(centerX - 3, 30, 6, 5);
			g.drawLine(centerX - 6, 39, centerX - 6, 44);
			g.drawLine(centerX, 39, centerX, 45);
			g.drawLine(centerX + 6, 39, centerX + 6, 44);

			g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor(SlayerPrepAssistantPanel.GOLD);
			g.drawArc(centerX - 31, 8, 22, 28, 105, 105);
			g.drawArc(centerX + 9, 8, 22, 28, -30, 105);
		}
		finally
		{
			g.dispose();
		}
	}
}