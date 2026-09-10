package com.slayerprepassistant.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

class CollapsibleSection extends RoundedPanel
{
	private final String title;
	private final String summary;
	private final JPanel body;
	private final Runnable refresh;
	private boolean open;

	CollapsibleSection(String title, String summary, boolean defaultOpen, JPanel body, Runnable refresh)
	{
		super(new BorderLayout(0, 5), SlayerPrepAssistantPanel.PANEL, SlayerPrepAssistantPanel.CARD_RADIUS);
		this.title = title;
		this.summary = summary;
		this.body = body;
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
			refresh.run();
		});
		add(header, BorderLayout.NORTH);
		body.setVisible(open);
		add(body, BorderLayout.CENTER);
	}

	private JButton headerButton()
	{
		JButton button = new JButton(headerText());
		button.setForeground(SlayerPrepAssistantPanel.TEXT);
		button.setBackground(SlayerPrepAssistantPanel.PANEL_LIGHT);
		button.setFont(button.getFont().deriveFont(Font.PLAIN, SlayerPrepAssistantPanel.FONT_SM));
		button.setFocusPainted(false);
		button.setOpaque(false);
		button.setContentAreaFilled(false);
		button.setBorder(BorderFactory.createCompoundBorder(
			new RoundedBorder(SlayerPrepAssistantPanel.BORDER, SlayerPrepAssistantPanel.CONTROL_RADIUS),
			BorderFactory.createEmptyBorder(4, 6, 4, 6)));
		button.setHorizontalAlignment(SwingConstants.LEFT);
		button.setMaximumSize(new Dimension(Integer.MAX_VALUE, button.getPreferredSize().height));
		return button;
	}

	private String headerText()
	{
		return title + (summary == null || summary.isEmpty() ? "" : "  " + summary) + "  " + (open ? "v" : ">");
	}
}
