package com.slayerprepassistant.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import javax.swing.JPanel;

class RoundedPanel extends JPanel
{
	private final Color background;
	private final int radius;

	RoundedPanel(LayoutManager layout, Color background, int radius)
	{
		super(layout);
		this.background = background == null ? Color.DARK_GRAY : background;
		this.radius = Math.max(0, radius);
		setOpaque(false);
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
			g.setColor(background);
			g.fillRoundRect(0, 0, width, height, radius, radius);
		}
		finally
		{
			g.dispose();
		}
	}
}