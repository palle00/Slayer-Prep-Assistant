package com.slayerprepassistant.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.border.Border;

class RoundedBorder implements Border
{
	private final Color color;
	private final int radius;

	RoundedBorder(Color color, int radius)
	{
		this.color = color;
		this.radius = radius;
	}

	@Override
	public Insets getBorderInsets(Component component)
	{
		return new Insets(1, 1, 1, 1);
	}

	@Override
	public boolean isBorderOpaque()
	{
		return false;
	}

	@Override
	public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(color);
		g.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
		g.dispose();
	}
}
