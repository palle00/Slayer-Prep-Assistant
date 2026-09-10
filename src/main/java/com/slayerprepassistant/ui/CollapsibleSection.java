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
		this.title = title == null ? "" : title;
		this.summary = summary == null ? "" : summary;
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
			if (this.refresh != null)
			{
				this.refresh.run();
			}
		});
		add(header, BorderLayout.NORTH);
		this.body.setVisible(open);
		add(this.body, BorderLayout.CENTER);
	}

	private JButton headerButton()
	{
		JButton button = new JButton(headerText());
		button.setForeground(SlayerPrepAssistantPanel.TEXT);
		button.setBackground(SlayerPrepAssistantPanel.PANEL_LIGHT);

		Font baseFont = button.getFont();
		if (baseFont != null)
		{
			button.setFont(baseFont.deriveFont(Font.PLAIN, SlayerPrepAssistantPanel.FONT_SM));
		}

		button.setFocusPainted(false);
		button.setOpaque(false);
		button.setContentAreaFilled(false);
		button.setBorder(BorderFactory.createCompoundBorder(
				new RoundedBorder(SlayerPrepAssistantPanel.BORDER, SlayerPrepAssistantPanel.CONTROL_RADIUS),
				BorderFactory.createEmptyBorder(4, 6, 4, 6)));
		button.setHorizontalAlignment(SwingConstants.LEFT);

		Dimension prefSize = button.getPreferredSize();
		int prefHeight = prefSize != null ? prefSize.height : 24;
		button.setMaximumSize(new Dimension(Integer.MAX_VALUE, prefHeight));
		return button;
	}

	private String headerText()
	{
		String safeTitle = title == null ? "" : title;
		String safeSummary = summary == null || summary.isEmpty() ? "" : "  " + summary;
		String arrow = open ? "v" : ">";
		return safeTitle + safeSummary + "  " + arrow;
	}
}