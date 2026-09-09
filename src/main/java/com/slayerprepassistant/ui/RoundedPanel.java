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
		this.background = background;
		this.radius = radius;
		setOpaque(false);
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D g = (Graphics2D) graphics.create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(background);
		g.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
		g.dispose();
		super.paintComponent(graphics);
	}
}
