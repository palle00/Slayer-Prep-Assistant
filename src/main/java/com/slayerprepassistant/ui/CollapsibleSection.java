package com.slayerprepassistant.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

class CollapsibleSection extends RoundedPanel
{
	private final String title;
	private final String summary;
	private final Icon icon;
	private final JPanel body;
	private final Runnable refresh;
	private boolean open;

	CollapsibleSection(String title, String summary, boolean defaultOpen, JPanel body, Runnable refresh)
	{
		this(title, summary, null, defaultOpen, body, refresh);
	}

	CollapsibleSection(String title, String summary, Icon icon, boolean defaultOpen, JPanel body, Runnable refresh)
	{
		super(new BorderLayout(0, 5), SlayerPrepAssistantPanel.PANEL, SlayerPrepAssistantPanel.CARD_RADIUS);
		this.title = title == null ? "" : title;
		this.summary = summary == null ? "" : summary;
		this.icon = icon;
		this.body = body == null ? new JPanel() : body;
		this.refresh = refresh;
		this.open = defaultOpen;

		setBorder(BorderFactory.createCompoundBorder(
				new RoundedBorder(SlayerPrepAssistantPanel.BORDER, SlayerPrepAssistantPanel.CARD_RADIUS),
				BorderFactory.createEmptyBorder(7, 8, 7, 8)));
		setAlignmentX(Component.LEFT_ALIGNMENT);

		JButton header = headerButton();
		header.addActionListener(event ->
		{
			open = !open;
			this.body.setVisible(open);
			header.setText(headerText());
			updateHeight();
			if (this.refresh != null)
			{
				this.refresh.run();
			}
		});
		add(header, BorderLayout.NORTH);
		this.body.setVisible(open);
		add(this.body, BorderLayout.CENTER);
		updateHeight();
	}

	private JButton headerButton()
	{
		JButton button = new JButton(headerText());
		button.setIcon(icon);
		button.setIconTextGap(6);
		button.setForeground(SlayerPrepAssistantPanel.TEXT);
		button.setBackground(SlayerPrepAssistantPanel.PANEL_LIGHT);

		Font baseFont = button.getFont();
		if (baseFont != null)
		{
			button.setFont(baseFont.deriveFont(Font.PLAIN, SlayerPrepAssistantPanel.FONT_SM));
		}

		button.setFocusPainted(false);
		button.setOpaque(true);
		button.setContentAreaFilled(true);
		button.setBorder(headerBorder(SlayerPrepAssistantPanel.BORDER));
		button.setHorizontalAlignment(SwingConstants.LEFT);
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		button.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent event)
			{
				button.setBackground(new Color(34, 34, 34));
				button.setForeground(Color.WHITE);
				button.setBorder(headerBorder(new Color(118, 118, 118)));
			}

			@Override
			public void mouseExited(MouseEvent event)
			{
				button.setBackground(SlayerPrepAssistantPanel.PANEL_LIGHT);
				button.setForeground(SlayerPrepAssistantPanel.TEXT);
				button.setBorder(headerBorder(SlayerPrepAssistantPanel.BORDER));
			}
		});

		Dimension prefSize = button.getPreferredSize();
		int prefHeight = prefSize != null ? prefSize.height : 24;
		button.setMaximumSize(new Dimension(Integer.MAX_VALUE, prefHeight));
		return button;
	}

	private Border headerBorder(Color color)
	{
		return BorderFactory.createCompoundBorder(
				new RoundedBorder(color, SlayerPrepAssistantPanel.CONTROL_RADIUS),
				BorderFactory.createEmptyBorder(4, 6, 4, 6));
	}

	private String headerText()
	{
		String safeTitle = title == null ? "" : title;
		String safeSummary = summary == null || summary.isEmpty() ? "" : "  " + summary;
		String arrow = open ? "v" : ">";
		return safeTitle + safeSummary + "  " + arrow;
	}

	private void updateHeight()
	{
		revalidate();
		Dimension preferred = getPreferredSize();
		int height = preferred == null ? 0 : preferred.height;
		setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
		setMinimumSize(new Dimension(0, height));
	}
}
