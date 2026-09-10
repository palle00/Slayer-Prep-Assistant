package com.slayerprepassistant.ui;

import com.slayerprepassistant.bank.OwnershipState;
import com.slayerprepassistant.gear.GearMatch;
import com.slayerprepassistant.gear.GearMatcher;
import com.slayerprepassistant.gear.GearRecommendation;
import com.slayerprepassistant.gear.GearTier;
import com.slayerprepassistant.gear.LoadoutMode;
import com.slayerprepassistant.gear.RecommendedItem;
import com.slayerprepassistant.guide.CombatMethod;
import com.slayerprepassistant.guide.InventoryRecommendation;
import com.slayerprepassistant.items.ItemResolver;
import com.slayerprepassistant.items.RuneLiteItemLookup;
import com.slayerprepassistant.prep.PreparationResult;
import com.slayerprepassistant.prep.PreparationStatus;
import com.slayerprepassistant.task.SlayerTaskContext;
import com.slayerprepassistant.task.TargetOption;
import com.slayerprepassistant.wiki.WikiTitles;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

public class SlayerPrepAssistantPanel extends PluginPanel
{
	static final Color BACKGROUND = new Color(25, 25, 25);
	static final Color PANEL = new Color(30, 30, 30);
	static final Color PANEL_LIGHT = new Color(38, 38, 38);
	static final Color BORDER = new Color(44, 44, 44);
	static final Color TEXT = new Color(230, 230, 230);
	static final Color MUTED = new Color(168, 168, 168);
	static final Color GOLD = new Color(255, 176, 0);
	static final Color GREEN = new Color(128, 206, 82);
	static final Color BLUE = new Color(82, 168, 214);
	static final Color YELLOW = new Color(255, 207, 38);
	static final Color RED = new Color(221, 83, 72);
	private static final int SIDEBAR_WIDTH = 226;
	private static final int CONTROL_HEIGHT = 34;
	static final int CARD_RADIUS = 3;
	static final int CONTROL_RADIUS = 3;
	static final float FONT_XS = 13f;
	static final float FONT_SM = 14f;
	private static final float FONT_MD = 15f;
	private static final float FONT_LG = 16f;
	static final float FONT_XL = 17f;
	private static final float FONT_TITLE = 18f;
	private static final float FONT_TASK_TITLE = 19f;

	private final java.util.function.Consumer<TargetOption> targetConsumer;
	private final Runnable refreshConsumer;
	private final WikiImageLoader wikiImageLoader;
	private final ItemManager itemManager;
	private final ItemResolver itemResolver;
	private final RuneLiteItemLookup itemLookup;
	private final GearMatcher gearMatcher;
	private final BufferedImage pluginIcon;

	private final JPanel body = verticalPanel(BACKGROUND);
	private final JPanel controlsPanel = verticalPanel(PANEL);
	private JPanel targetBlock;
	private JPanel methodBlock;
	private JPanel loadoutBlock;
	private JPanel taskCardPanel;
	private Component methodSpacer;
	private Component loadoutSpacer;
	private final JPanel resultPanel = verticalPanel(BACKGROUND);
	private final JLabel taskStatusLabel = new JLabel("No active task");
	private final JLabel taskNameLabel = new JLabel("No Slayer task detected");
	private final JLabel taskMetaLabel = new JLabel("Check your Slayer helmet or gem.");
	private final JLabel taskIconLabel = new JLabel();
	private final JLabel targetLabel = new JLabel("No target selected");
	private final JPanel targetCards = new JPanel(new CardLayout());
	private final JComboBox<TargetOption> targetSelect = new JComboBox<>();
	private final JPanel methodPanel = new JPanel(new GridLayout(1, 1, 4, 0));
	private final JPanel loadoutPanel = new JPanel(new GridLayout(1, 2, 4, 0));
	private JComponent readinessPanel;
	private final CircularReadinessBadge readinessBadge = new CircularReadinessBadge();
	private final JButton openWikiButton = new JButton("Open Wiki Page");
	private final JComboBox<TargetOption> noSetupVariantSelect = new JComboBox<>();
	private final List<CombatMethod> methodChoices = new ArrayList<>();
	private final Timer skeletonTimer;

	private PreparationResult currentResult;
	private TargetOption selectedTarget;
	private CombatMethod selectedMethod = CombatMethod.defaultMethod();
	private LoadoutMode selectedLoadoutMode = LoadoutMode.BEST_I_OWN;
	private boolean rebuilding;
	private String currentWikiUrl = "";
	private String lastTaskRenderKey = "";
	private String lastTargetRenderKey = "";
	private String lastResultRenderKey = "";
	private BufferedImage displayedTaskImage;
	private int skeletonFrame;

	public SlayerPrepAssistantPanel(
		java.util.function.Consumer<TargetOption> targetConsumer,
		Runnable refreshConsumer,
		WikiImageLoader wikiImageLoader,
		ItemManager itemManager,
		ItemResolver itemResolver,
		RuneLiteItemLookup itemLookup,
		BufferedImage pluginIcon)
	{
		super(false);
		this.targetConsumer = targetConsumer;
		this.refreshConsumer = refreshConsumer;
		this.wikiImageLoader = wikiImageLoader;
		this.itemManager = itemManager;
		this.itemResolver = itemResolver;
		this.itemLookup = itemLookup;
		this.gearMatcher = new GearMatcher(itemResolver);
		this.pluginIcon = pluginIcon;
		this.skeletonTimer = new Timer(120, event ->
		{
			skeletonFrame = (skeletonFrame + 1) % 12;
			resultPanel.repaint();
		});
		noSetupVariantSelect.addActionListener(event ->
		{
			if (!rebuilding && noSetupVariantSelect.getSelectedItem() instanceof TargetOption)
			{
				TargetOption target = (TargetOption) noSetupVariantSelect.getSelectedItem();
				selectedTarget = target;
				currentWikiUrl = wikiUrl(target);
				targetConsumer.accept(target);
			}
		});

		setLayout(new BorderLayout());
		setBackground(BACKGROUND);
		setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));

		body.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		add(body, BorderLayout.NORTH);
		add(footerPanel(), BorderLayout.SOUTH);
		buildBaseLayout();
		showNoTask();
	}

	public void dispose()
	{
		stopSkeletonLoading();
	}

	public CombatMethod getSelectedMethod()
	{
		return selectedMethod;
	}

	public LoadoutMode getSelectedLoadoutMode()
	{
		return selectedLoadoutMode;
	}

	public void showTask(SlayerTaskContext taskContext, List<TargetOption> targets)
	{
		SwingUtilities.invokeLater(() ->
		{
			boolean activeTask = taskContext != null && taskContext.isActive();
			String taskName = activeTask ? taskContext.getTaskName() : "No Slayer task detected";
			String taskMeta = activeTask ? remainingText(taskContext) : "Check your Slayer helmet or gem.";
			String taskRenderKey = activeTask + "|" + taskName + "|" + taskMeta + "|" + targetsKey(targets);
			if (!taskRenderKey.equals(lastTaskRenderKey))
			{
				taskStatusLabel.setText(activeTask ? "Task detected" : "No active task");
				taskStatusLabel.setForeground(activeTask ? GREEN : MUTED);
				taskNameLabel.setText(taskName);
				taskMetaLabel.setText(taskMeta);
				lastTaskRenderKey = taskRenderKey;
			}
			if (taskCardPanel != null)
			{
				taskCardPanel.setVisible(activeTask);
			}
			rebuildTargetControl(targets, false);
			if (!activeTask && (targets == null || targets.isEmpty()))
			{
				showNoTask();
			}
		});
	}

	public void setTaskImage(BufferedImage image)
	{
		SwingUtilities.invokeLater(() ->
		{
			if (displayedTaskImage == image)
			{
				return;
			}
			displayedTaskImage = image;
			taskIconLabel.setIcon(new ImageIcon(ImageUtil.resizeImage(image == null ? pluginIcon : image, 48, 48)));
			refreshUi();
		});
	}

	public void showLoading(SlayerTaskContext taskContext, List<TargetOption> targets, TargetOption target)
	{
		SwingUtilities.invokeLater(() ->
		{
			currentResult = null;
			selectedTarget = target;
			lastResultRenderKey = "";
			rebuildTargetControl(targets, false);
			rebuildSkeletonLoading();
		});
	}

	public void showVariantSelection(SlayerTaskContext taskContext, List<TargetOption> variants, String message)
	{
		SwingUtilities.invokeLater(() ->
		{
			stopSkeletonLoading();
			currentResult = null;
			selectedTarget = null;
			rebuildTargetControl(variants, true);
			rebuildStatePanel("NO DIRECT SETUP", message, "Choose a monster variant above.");
		});
	}

	public void showPreparation(PreparationResult result)
	{
		SwingUtilities.invokeLater(() ->
		{
			String resultRenderKey = resultKey(result);
			if (resultRenderKey.equals(lastResultRenderKey) && !skeletonTimer.isRunning())
			{
				return;
			}
			lastResultRenderKey = resultRenderKey;
			stopSkeletonLoading();
			currentResult = result;
			selectedTarget = result == null ? selectedTarget : result.getSelectedTarget();
			rebuildResult();
		});
	}

	private void buildBaseLayout()
	{
		body.removeAll();
		body.add(headerPanel());
		body.add(spacer(5));
		taskCardPanel = taskCard();
		body.add(taskCardPanel);
		body.add(spacer(5));
		body.add(controlsPanel());
		body.add(spacer(5));
		body.add(resultPanel);
		fitHeight(body);
	}

	private JPanel headerPanel()
	{
		JPanel panel = card(new BorderLayout(8, 0));
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		if (pluginIcon != null)
		{
			panel.add(new JLabel(new ImageIcon(pluginIcon)), BorderLayout.WEST);
		}

		JPanel textPanel = verticalPanel(PANEL);
		JLabel title = new JLabel("Slayer Prep Assistant");
		title.setForeground(GOLD);
		title.setFont(title.getFont().deriveFont(Font.BOLD, FONT_TITLE));
		taskStatusLabel.setFont(taskStatusLabel.getFont().deriveFont(Font.PLAIN, FONT_SM));
		textPanel.add(title);
		textPanel.add(taskStatusLabel);
		panel.add(textPanel, BorderLayout.CENTER);
		fitHeight(panel);
		return panel;
	}

	private JPanel taskCard()
	{
		JPanel panel = card(new BorderLayout(8, 0));
		JPanel row = new JPanel(new BorderLayout(8, 0));
		row.setBackground(PANEL);
		taskIconLabel.setIcon(new ImageIcon(ImageUtil.resizeImage(pluginIcon, 48, 48)));
		taskIconLabel.setHorizontalAlignment(SwingConstants.CENTER);
		taskIconLabel.setPreferredSize(new Dimension(56, 56));
		taskIconLabel.setBorder(BorderFactory.createLineBorder(new Color(48, 48, 48)));
		row.add(taskIconLabel, BorderLayout.WEST);

		JPanel text = verticalPanel(PANEL);
		JLabel caption = label("Current Task", MUTED, Font.PLAIN, FONT_XS);
		taskNameLabel.setForeground(TEXT);
		taskNameLabel.setFont(taskNameLabel.getFont().deriveFont(Font.BOLD, FONT_TASK_TITLE));
		taskMetaLabel.setForeground(MUTED);
		taskMetaLabel.setFont(taskMetaLabel.getFont().deriveFont(Font.PLAIN, FONT_LG));
		text.add(caption);
		text.add(taskNameLabel);
		text.add(taskMetaLabel);
		row.add(text, BorderLayout.CENTER);
		row.add(readinessBadge, BorderLayout.EAST);
		panel.add(row, BorderLayout.CENTER);
		readinessPanel = readinessBadge;
		fitHeight(panel);
		return panel;
	}

	private JPanel controlsPanel()
	{
		controlsPanel.removeAll();
		controlsPanel.setBorder(compoundBorder());
		targetBlock = controlRow("Target Variant", targetControlPanel());
		methodBlock = controlRow("Combat Style", methodPanel);
		loadoutBlock = controlRow("Loadout Mode", loadoutPanel);
		methodSpacer = spacer(4);
		loadoutSpacer = spacer(4);
		styleCombo(targetSelect);
		controlsPanel.add(targetBlock);
		controlsPanel.add(methodSpacer);
		controlsPanel.add(methodBlock);
		controlsPanel.add(loadoutSpacer);
		controlsPanel.add(loadoutBlock);
		rebuildLoadoutPanel();
		fitHeight(controlsPanel);
		return controlsPanel;
	}

	private JPanel targetControlPanel()
	{
		JPanel panel = targetCards;
		panel.setBackground(PANEL);
		targetLabel.setForeground(TEXT);
		targetLabel.setFont(targetLabel.getFont().deriveFont(Font.PLAIN, FONT_MD));
		targetLabel.setBorder(controlBorder(false));
		targetSelect.addActionListener(event ->
		{
			if (!rebuilding && targetSelect.getSelectedItem() instanceof TargetOption)
			{
				targetConsumer.accept((TargetOption) targetSelect.getSelectedItem());
			}
		});
		panel.add(targetLabel, "label");
		panel.add(targetSelect, "select");
		return panel;
	}

	private JPanel controlRow(String title, Component component)
	{
		JPanel wrapper = new JPanel(new BorderLayout());
		wrapper.setBackground(PANEL);
		wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
		component.setMaximumSize(new Dimension(Integer.MAX_VALUE, CONTROL_HEIGHT));
		component.setPreferredSize(new Dimension(1, CONTROL_HEIGHT));
		wrapper.add(component, BorderLayout.CENTER);
		fitHeight(wrapper);
		return wrapper;
	}

	private JPanel footerPanel()
	{
		JPanel footer = new JPanel(new BorderLayout());
		footer.setBackground(BACKGROUND);
		footer.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));

		styledButton(openWikiButton);
		openWikiButton.setPreferredSize(new Dimension(1, CONTROL_HEIGHT));
		openWikiButton.addActionListener(event -> openCurrentWiki());
		footer.add(openWikiButton, BorderLayout.CENTER);
		return footer;
	}

	private void openCurrentWiki()
	{
		String url = currentWikiUrl;
		if ((url == null || url.isEmpty()) && currentResult != null && currentResult.getGuide() != null)
		{
			url = currentResult.getGuide().getWikiUrl();
		}
		if ((url == null || url.isEmpty()) && selectedTarget != null)
		{
			url = wikiUrl(selectedTarget);
		}
		Object noSetupTarget = noSetupVariantSelect.getSelectedItem();
		if ((url == null || url.isEmpty()) && noSetupTarget instanceof TargetOption)
		{
			url = wikiUrl((TargetOption) noSetupTarget);
		}
		Object dropdownTarget = targetSelect.getSelectedItem();
		if ((url == null || url.isEmpty()) && dropdownTarget instanceof TargetOption)
		{
			url = wikiUrl((TargetOption) dropdownTarget);
		}
		if (url != null && !url.isEmpty())
		{
			LinkBrowser.browse(url);
		}
	}

	public void showNoTask()
	{
		stopSkeletonLoading();
		currentResult = null;
		selectedTarget = null;
		currentWikiUrl = "";
		lastResultRenderKey = "";
		rebuildTargetControl(Collections.emptyList(), false);
		rebuildReadiness(0, false);
		showSetupControls(false);
		if (taskCardPanel != null)
		{
			taskCardPanel.setVisible(false);
		}
		controlsPanel.setVisible(false);
		resultPanel.removeAll();
		refreshUi();
	}

	private void rebuildResult()
	{
		stopSkeletonLoading();
		resultPanel.removeAll();
		if (currentResult == null)
		{
			rebuildReadiness(0, false);
			showSetupControls(false);
			rebuildStatePanel("NO SETUP", "Select a target to build a checklist.", null);
			return;
		}
		if (currentResult.getStatus() == PreparationStatus.NO_SETUP)
		{
			rebuildReadiness(0, false);
			showSetupControls(false);
			showTargetControl(false);
			controlsPanel.setVisible(false);
			rebuildNoSetupPanel();
			return;
		}

		currentWikiUrl = currentResult.getGuide().getWikiUrl();
		showTargetControl(true);
		showSetupControls(true);
		updateMethodChoices();
		rebuildReadiness(currentResult.getReadinessResult().getPercentage(), true);
		resultPanel.add(equipmentSection());
		resultPanel.add(spacer(5));
		resultPanel.add(inventorySection());
		refreshUi();
	}

	private void rebuildStatePanel(String title, String message, String detail)
	{
		stopSkeletonLoading();
		currentWikiUrl = selectedTarget == null ? "" : wikiUrl(selectedTarget);
		resultPanel.removeAll();
		JPanel state = card(new BorderLayout(0, 6));
		state.add(label(title, GOLD, Font.BOLD, FONT_SM), BorderLayout.NORTH);
		JPanel lines = verticalPanel(PANEL);
		lines.add(wrapped(message, TEXT));
		if (detail != null && !detail.isEmpty())
		{
			lines.add(spacer(4));
			lines.add(wrapped(detail, MUTED));
		}
		state.add(lines, BorderLayout.CENTER);
		fitHeight(state);
		resultPanel.add(state);
		refreshUi();
	}

	private void rebuildNoSetupPanel()
	{
		stopSkeletonLoading();
		if (selectedTarget == null && currentResult != null && !currentResult.getTargets().isEmpty())
		{
			selectedTarget = currentResult.getTargets().get(0);
		}
		currentWikiUrl = selectedTarget == null ? "" : wikiUrl(selectedTarget);
		resultPanel.removeAll();

		JPanel state = card(new BorderLayout(0, 8));
		state.setBorder(BorderFactory.createCompoundBorder(
			roundedBorder(new Color(82, 82, 82), CARD_RADIUS),
			BorderFactory.createEmptyBorder(12, 8, 12, 8)));
		JComponent icon = new NoSetupIcon();
		icon.setAlignmentX(Component.CENTER_ALIGNMENT);
		state.add(icon, BorderLayout.NORTH);

		JPanel text = verticalPanel(PANEL);
		JLabel title = label("No setup found", GOLD, Font.BOLD, FONT_XL);
		title.setHorizontalAlignment(SwingConstants.CENTER);
		title.setAlignmentX(Component.CENTER_ALIGNMENT);
		JLabel message = centeredWrapped(currentResult.getMessage(), TEXT);
		message.setHorizontalAlignment(SwingConstants.CENTER);
		message.setAlignmentX(Component.CENTER_ALIGNMENT);
		text.add(title);
		text.add(spacer(5));
		text.add(message);
		state.add(text, BorderLayout.CENTER);
		fitHeight(state);
		resultPanel.add(state);

		if (currentResult.getTargets().size() > 1)
		{
			resultPanel.add(spacer(5));
			resultPanel.add(noSetupVariantsPanel());
		}

		refreshUi();
	}

	private JPanel noSetupVariantsPanel()
	{
		JPanel panel = card(new BorderLayout(0, 7));
		JPanel hint = new JPanel(new BorderLayout(7, 0));
		hint.setBackground(PANEL);
		hint.add(label("i", BLUE, Font.BOLD, FONT_MD), BorderLayout.WEST);
		hint.add(wrapped("This monster has variants you can try", MUTED), BorderLayout.CENTER);
		panel.add(hint, BorderLayout.NORTH);

		rebuilding = true;
		noSetupVariantSelect.removeAllItems();
		for (TargetOption target : currentResult.getTargets())
		{
			noSetupVariantSelect.addItem(target);
		}
		if (selectedTarget != null && currentResult.getTargets().contains(selectedTarget))
		{
			noSetupVariantSelect.setSelectedItem(selectedTarget);
		}
		else
		{
			noSetupVariantSelect.setSelectedIndex(0);
		}
		rebuilding = false;
		styleCombo(noSetupVariantSelect);
		panel.add(noSetupVariantSelect, BorderLayout.CENTER);
		fitHeight(panel);
		return panel;
	}

	private void rebuildSkeletonLoading()
	{
		currentWikiUrl = selectedTarget == null ? "" : wikiUrl(selectedTarget);
		resultPanel.removeAll();
		resultPanel.add(new SkeletonSection("EQUIPMENT", 8, true, () -> skeletonFrame));
		resultPanel.add(spacer(5));
		resultPanel.add(new SkeletonSection("INVENTORY", 2, false, () -> skeletonFrame));
		startSkeletonLoading();
		refreshUi();
	}

	private void startSkeletonLoading()
	{
		if (!skeletonTimer.isRunning())
		{
			skeletonTimer.start();
		}
	}

	private void stopSkeletonLoading()
	{
		if (skeletonTimer.isRunning())
		{
			skeletonTimer.stop();
		}
	}

	private String wikiUrl(TargetOption target)
	{
		if (target == null || target.getWikiPage() == null || target.getWikiPage().trim().isEmpty())
		{
			return "";
		}
		return WikiTitles.pageUrl(target.getWikiPage());
	}

	private String targetsKey(List<TargetOption> targets)
	{
		if (targets == null || targets.isEmpty())
		{
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (TargetOption target : targets)
		{
			if (builder.length() > 0)
			{
				builder.append(';');
			}
			builder.append(TargetOption.lookupKey(target));
		}
		return builder.toString();
	}

	private String resultKey(PreparationResult result)
	{
		if (result == null)
		{
			return "";
		}
		StringBuilder builder = new StringBuilder();
		builder.append(result.getStatus()).append('|')
			.append(result.getMessage()).append('|')
			.append(TargetOption.lookupKey(result.getSelectedTarget())).append('|')
			.append(result.getSelectedMethod());
		if (result.getLoadoutResult() != null)
		{
			builder.append('|').append(result.getLoadoutResult().getMode());
			for (GearMatch match : result.getLoadoutResult().getGearMatches())
			{
				builder.append('|')
					.append(match.getSlot()).append(':')
					.append(match.getItem().getName()).append(':')
					.append(match.getOwnershipState()).append(':')
					.append(match.getTierPriority());
			}
		}
		if (result.getStrategyMethod() != null)
		{
			for (InventoryRecommendation recommendation : result.getStrategyMethod().getInventory())
			{
				builder.append('|')
					.append(recommendation.getItemOrCategory()).append(':')
					.append(recommendation.getMinimumQuantity()).append(':')
					.append(inventoryOwnership(recommendation, result.getPlayerState()));
			}
		}
		return builder.toString();
	}

	private JPanel equipmentSection()
	{
		JPanel rows = verticalPanel(PANEL);
		if (shouldShowBankDataHint())
		{
			rows.add(wrapped("Open your bank once to load owned gear.", YELLOW));
			rows.add(spacer(4));
		}
		List<GearMatch> matches = currentResult.getLoadoutResult().getGearMatches();
		for (int i = 0; i < matches.size(); i++)
		{
			rows.add(gearRow(matches.get(i), i));
		}
		return new CollapsibleSection("EQUIPMENT", null, true, rows, this::refreshUi);
	}

	private JPanel gearRow(GearMatch match, int index)
	{
		Color rowBackground = index % 2 == 0 ? new Color(32, 32, 32) : new Color(27, 27, 27);
		JPanel wrapper = verticalPanel(rowBackground);
		wrapper.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
			BorderFactory.createEmptyBorder(0, 3, 0, 0)));
		JPanel row = new JPanel(new BorderLayout(7, 0));
		row.setBackground(rowBackground);
		row.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
		row.add(itemIcon(match.getItem(), 30), BorderLayout.WEST);

		JPanel text = verticalPanel(rowBackground);
		text.add(label(match.getSlot().displayName().toUpperCase(), MUTED, Font.BOLD, FONT_XS));
		text.add(label(match.getItem().getName(), TEXT, Font.PLAIN, FONT_SM));
		row.add(text, BorderLayout.CENTER);
		JLabel status = label(displayStatusText(match.getOwnershipState()), statusColor(match.getOwnershipState()), Font.BOLD, FONT_XS);
		status.setHorizontalAlignment(SwingConstants.RIGHT);
		status.setPreferredSize(new Dimension(54, 20));
		row.add(status, BorderLayout.EAST);
		wrapper.add(row);

		JPanel alternatives = alternativesPanel(match);
		alternatives.setVisible(false);
		wrapper.add(alternatives);
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		row.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mouseClicked(java.awt.event.MouseEvent event)
			{
				alternatives.setVisible(!alternatives.isVisible());
				refreshUi();
			}
		});
		return wrapper;
	}

	private JPanel alternativesPanel(GearMatch match)
	{
		JPanel panel = verticalPanel(PANEL_LIGHT);
		panel.setBorder(BorderFactory.createEmptyBorder(4, 34, 6, 0));
		findRecommendation(match).ifPresent(recommendation ->
		{
			for (GearTier tier : recommendation.getTiers())
			{
				for (RecommendedItem item : tier.getAlternatives())
				{
					JPanel row = new JPanel(new BorderLayout(5, 0));
					row.setBackground(PANEL_LIGHT);
					JLabel rank = label(tier.getPriority() == match.getTierPriority() && item.getName().equals(match.getItem().getName()) ? "*" : String.valueOf(tier.getPriority()), GOLD, Font.BOLD, FONT_XS);
					rank.setPreferredSize(new Dimension(12, 18));
					row.add(rank, BorderLayout.WEST);
					JPanel itemPanel = new JPanel(new BorderLayout(5, 0));
					itemPanel.setBackground(PANEL_LIGHT);
					itemPanel.add(itemIcon(item, 20), BorderLayout.WEST);
					itemPanel.add(label(item.getName(), TEXT, Font.PLAIN, FONT_XS), BorderLayout.CENTER);
					row.add(itemPanel, BorderLayout.CENTER);
					OwnershipState state = itemOwnership(item);
					row.add(label(displayStatusText(state), statusColor(state), Font.BOLD, FONT_XS), BorderLayout.EAST);
					panel.add(row);
				}
			}
		});
		if (panel.getComponentCount() == 0)
		{
			panel.add(label("No alternatives parsed.", MUTED, Font.PLAIN, FONT_XS));
		}
		return panel;
	}

	private Optional<GearRecommendation> findRecommendation(GearMatch match)
	{
		return currentResult.getStrategyMethod().getGear().stream()
			.filter(recommendation -> recommendation.getSlot() == match.getSlot())
			.findFirst();
	}

	private JPanel inventorySection()
	{
		JPanel body = verticalPanel(PANEL);
		JPanel grid = new JPanel(new GridLayout(0, 4, 5, 5));
		grid.setBackground(PANEL);
		for (InventoryRecommendation recommendation : currentResult.getStrategyMethod().getInventory())
		{
			grid.add(inventorySlot(recommendation));
		}
		int slots = currentResult.getStrategyMethod().getInventory().size();
		while (grid.getComponentCount() < Math.max(8, Math.min(28, slots)))
		{
			grid.add(emptySlot());
		}
		body.add(grid);
		body.add(spacer(5));
		body.add(label(slots + " / 28 recommended", MUTED, Font.PLAIN, FONT_XS));
		return new CollapsibleSection("INVENTORY", null, true, body, this::refreshUi);
	}

	private JComponent inventorySlot(InventoryRecommendation recommendation)
	{
		OwnershipState state = inventoryOwnership(recommendation);
		RecommendedItem item = itemResolver.resolve(recommendation.getItemOrCategory());
		InventorySlotTile slot = new InventorySlotTile(item, recommendation.getMinimumQuantity(), state, this::itemImage, this::loadWikiItemImage);
		slot.setToolTipText(recommendation.getItemOrCategory() + " - " + displayStatusText(state));
		return slot;
	}

	private JComponent emptySlot()
	{
		return new InventorySlotTile(null, 0, null, this::itemImage, this::loadWikiItemImage);
	}

	private void rebuildTargetControl(List<TargetOption> targets, boolean awaitSelection)
	{
		List<TargetOption> safeTargets = targets == null ? Collections.emptyList() : targets;
		String targetRenderKey = awaitSelection + "|" + TargetOption.lookupKey(selectedTarget) + "|" + targetsKey(safeTargets);
		if (targetRenderKey.equals(lastTargetRenderKey))
		{
			return;
		}
		lastTargetRenderKey = targetRenderKey;
		rebuilding = true;
		targetSelect.removeAllItems();
		for (TargetOption target : safeTargets)
		{
			targetSelect.addItem(target);
		}
		boolean showDropdown = safeTargets.size() > 1 || awaitSelection;
		((CardLayout) targetCards.getLayout()).show(targetCards, showDropdown ? "select" : "label");
		if (showDropdown)
		{
			if (awaitSelection)
			{
				targetSelect.setSelectedIndex(-1);
			}
			else if (selectedTarget != null && safeTargets.contains(selectedTarget))
			{
				targetSelect.setSelectedItem(selectedTarget);
			}
			else if (!safeTargets.isEmpty())
			{
				targetSelect.setSelectedIndex(0);
			}
		}
		targetLabel.setText(safeTargets.isEmpty() ? "No target selected" : safeTargets.get(0).getDisplayName());
		rebuilding = false;
		boolean noSetupState = currentResult != null && currentResult.getStatus() == PreparationStatus.NO_SETUP;
		showTargetControl(!noSetupState);
		controlsPanel.setVisible(!safeTargets.isEmpty() && !noSetupState);
		if (!safeTargets.isEmpty() && currentResult == null)
		{
			showSetupControls(false);
		}
		fitHeight(controlsPanel);
		refreshUi();
	}

	private void updateMethodChoices()
	{
		methodChoices.clear();
		currentResult.getGuide().getMethods().forEach(method -> methodChoices.add(method.getMethod()));
		if (!methodChoices.contains(currentResult.getSelectedMethod()) && !methodChoices.isEmpty())
		{
			selectedMethod = methodChoices.get(0);
		}
		else
		{
			selectedMethod = currentResult.getSelectedMethod();
		}
		rebuildMethodPanel();
	}

	private void rebuildMethodPanel()
	{
		methodPanel.removeAll();
		methodPanel.setBackground(PANEL);
		methodPanel.setLayout(new GridLayout(1, Math.max(1, methodChoices.size()), 4, 0));
		if (methodChoices.size() <= 1)
		{
			methodPanel.add(label(methodChoices.isEmpty() ? CombatMethod.defaultMethod().toString() : methodChoices.get(0).toString(), TEXT, Font.PLAIN, FONT_XS));
			return;
		}
		for (CombatMethod method : methodChoices)
		{
			JButton button = styledButton(methodLabel(method));
			setMethodIcon(button, method);
			button.setFont(button.getFont().deriveFont(Font.PLAIN, FONT_XS));
			button.setBorder(BorderFactory.createCompoundBorder(
				roundedBorder(method == selectedMethod ? GOLD : BORDER, CONTROL_RADIUS),
				BorderFactory.createEmptyBorder(3, 3, 3, 3)));
			button.addActionListener(event ->
			{
				selectedMethod = method;
				rebuildMethodPanel();
				refreshConsumer.run();
			});
			methodPanel.add(button);
		}
	}

	private void rebuildLoadoutPanel()
	{
		loadoutPanel.removeAll();
		loadoutPanel.setBackground(PANEL);
		loadoutPanel.add(loadoutButton(LoadoutMode.BEST_I_OWN));
		loadoutPanel.add(loadoutButton(LoadoutMode.MAX));
	}

	private JButton loadoutButton(LoadoutMode mode)
	{
		JButton button = styledButton(mode.getDisplayName());
		button.setBorder(BorderFactory.createCompoundBorder(
			roundedBorder(mode == selectedLoadoutMode ? GOLD : BORDER, CONTROL_RADIUS),
			BorderFactory.createEmptyBorder(3, 3, 3, 3)));
		button.addActionListener(event ->
		{
			if (selectedLoadoutMode == mode)
			{
				return;
			}
			selectedLoadoutMode = mode;
			rebuildLoadoutPanel();
			refreshConsumer.run();
		});
		return button;
	}

	private void rebuildReadiness(int percent, boolean visible)
	{
		if (readinessPanel != null)
		{
			readinessPanel.setVisible(visible);
		}
		readinessBadge.setReadiness(visible ? percent : 0, visible);
	}

	private void showSetupControls(boolean visible)
	{
		if (methodBlock != null)
		{
			methodBlock.setVisible(visible);
		}
		if (methodSpacer != null)
		{
			methodSpacer.setVisible(visible);
		}
		if (loadoutBlock != null)
		{
			loadoutBlock.setVisible(visible);
		}
		if (loadoutSpacer != null)
		{
			loadoutSpacer.setVisible(visible);
		}
		fitHeight(controlsPanel);
	}

	private void showTargetControl(boolean visible)
	{
		if (targetBlock != null)
		{
			targetBlock.setVisible(visible);
		}
	}

	private String remainingText(SlayerTaskContext taskContext)
	{
		String amount = taskContext.getRemainingAmount() > 0 ? taskContext.getRemainingAmount() + " remaining" : "Remaining count unknown";
		return taskContext.getAssignedLocation().isEmpty() ? amount : amount + " at " + taskContext.getAssignedLocation();
	}

	private OwnershipState inventoryOwnership(InventoryRecommendation recommendation)
	{
		return inventoryOwnership(recommendation, currentResult.getPlayerState());
	}

	private OwnershipState inventoryOwnership(InventoryRecommendation recommendation, com.slayerprepassistant.bank.PlayerInventoryState playerState)
	{
		return gearMatcher.ownershipForInventoryItem(itemResolver.resolve(recommendation.getItemOrCategory()), playerState);
	}

	private OwnershipState itemOwnership(RecommendedItem item)
	{
		return gearMatcher.ownershipFor(item, currentResult.getPlayerState());
	}

	private boolean hasBankData()
	{
		return currentResult != null && currentResult.getPlayerState().getBankSnapshot().isKnown();
	}

	private boolean shouldShowBankDataHint()
	{
		return selectedLoadoutMode == LoadoutMode.BEST_I_OWN && !hasBankData();
	}

	private String displayStatusText(OwnershipState state)
	{
		return state == OwnershipState.UNKNOWN && shouldShowBankDataHint() ? "Bank?" : statusText(state);
	}

	private JLabel itemIcon(RecommendedItem item, int size)
	{
		Integer itemId = resolveItemId(item);
		if (itemId != null)
		{
			JLabel label = new JLabel();
			AsyncBufferedImage image = itemManager.getImage(itemId, size, false);
			image.addTo(label);
			return label;
		}
		JLabel fallback = label(item == null || item.getName().isEmpty() ? "" : item.getName().substring(0, 1).toUpperCase(java.util.Locale.ROOT), MUTED, Font.BOLD, FONT_XS);
		fallback.setHorizontalAlignment(SwingConstants.CENTER);
		fallback.setPreferredSize(new Dimension(size, size));
		fallback.setBorder(BorderFactory.createLineBorder(BORDER));
		loadWikiItemImage(item, size, image ->
		{
			fallback.setIcon(new ImageIcon(ImageUtil.resizeImage(image, size, size)));
			fallback.setText("");
			fallback.setBorder(null);
			fallback.revalidate();
			fallback.repaint();
		});
		return fallback;
	}

	private Image itemImage(RecommendedItem item, int size)
	{
		Integer itemId = resolveItemId(item);
		return itemId == null ? null : itemManager.getImage(itemId, size, false);
	}

	private void loadWikiItemImage(RecommendedItem item, int size, Consumer<BufferedImage> callback)
	{
		if (item == null || item.getName() == null || item.getName().trim().isEmpty() || wikiImageLoader == null)
		{
			return;
		}
		wikiImageLoader.load(item.getName(), size, image ->
		{
			if (image != null)
			{
				SwingUtilities.invokeLater(() -> callback.accept(image));
			}
		});
	}

	private Integer resolveItemId(RecommendedItem item)
	{
		if (item == null || item.getName().trim().isEmpty())
		{
			return null;
		}
		return itemLookup == null ? null : itemLookup.itemId(item);
	}

	private static JPanel verticalPanel(Color background)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBackground(background);
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		return panel;
	}

	private JPanel card(java.awt.LayoutManager layout)
	{
		JPanel panel = new RoundedPanel(layout, PANEL, CARD_RADIUS);
		panel.setBorder(compoundBorder());
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		fitHeight(panel);
		return panel;
	}

	private void fitHeight(Component component)
	{
		Dimension preferred = component.getPreferredSize();
		component.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferred.height));
	}

	private JLabel label(String text, Color color, int style, float size)
	{
		JLabel label = new JLabel(text == null ? "" : text);
		label.setForeground(color);
		label.setFont(label.getFont().deriveFont(style, size));
		return label;
	}

	private JLabel wrapped(String text, Color color)
	{
		JLabel label = label("<html><body style='width:180px'>" + escape(text) + "</body></html>", color, Font.PLAIN, FONT_XS);
		return label;
	}

	private JLabel centeredWrapped(String text, Color color)
	{
		return label("<html><body style='width:180px; text-align:center'>" + escape(text) + "</body></html>", color, Font.PLAIN, FONT_XS);
	}

	private String escape(String text)
	{
		return (text == null ? "" : text)
			.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;");
	}

	private JButton styledButton(String text)
	{
		return styledButton(new JButton(text));
	}

	private JButton styledButton(JButton button)
	{
		button.setForeground(TEXT);
		button.setBackground(PANEL_LIGHT);
		button.setFont(button.getFont().deriveFont(Font.PLAIN, FONT_SM));
		button.setFocusPainted(false);
		button.setOpaque(false);
		button.setContentAreaFilled(false);
		button.setBorder(controlBorder(false));
		return button;
	}

	private void styleCombo(JComboBox<?> comboBox)
	{
		comboBox.setForeground(TEXT);
		comboBox.setBackground(new Color(18, 18, 18));
		comboBox.setFont(comboBox.getFont().deriveFont(Font.PLAIN, FONT_SM));
		comboBox.setBorder(controlBorder(false));
		comboBox.setFocusable(false);
	}

	private Border controlBorder(boolean selected)
	{
		return BorderFactory.createCompoundBorder(
			roundedBorder(selected ? GOLD : BORDER, CONTROL_RADIUS),
			BorderFactory.createEmptyBorder(4, 6, 4, 6));
	}

	private String methodLabel(CombatMethod method)
	{
		switch (method)
		{
			case MELEE:
				return "Melee";
			case RANGED:
				return "Ranged";
			case MAGIC:
				return "Magic";
			default:
				return method.toString();
		}
	}

	private void setMethodIcon(JButton button, CombatMethod method)
	{
		String itemName;
		switch (method)
		{
			case MELEE:
				itemName = "Abyssal whip";
				break;
			case RANGED:
				itemName = "Rune crossbow";
				break;
			case MAGIC:
				itemName = "Wizard hat";
				break;
			default:
				itemName = "";
				break;
		}
		Image image = itemName.isEmpty() ? null : itemImage(new RecommendedItem(itemName), 18);
		if (image != null)
		{
			button.setIcon(new ImageIcon(image));
			button.setIconTextGap(4);
		}
	}

	private Border roundedBorder(Color color, int radius)
	{
		return new RoundedBorder(color, radius);
	}

	private Border compoundBorder()
	{
		return BorderFactory.createCompoundBorder(roundedBorder(BORDER, CARD_RADIUS), BorderFactory.createEmptyBorder(7, 8, 7, 8));
	}

	private Color statusColor(OwnershipState state)
	{
		switch (state)
		{
			case EQUIPPED:
				return GREEN;
			case OWNED_IN_INVENTORY:
				return BLUE;
			case OWNED_IN_BANK:
				return YELLOW;
			case MISSING:
				return RED;
			case UNKNOWN:
			default:
				return MUTED;
		}
	}

	private String statusText(OwnershipState state)
	{
		switch (state)
		{
			case EQUIPPED:
				return "Equipped";
			case OWNED_IN_INVENTORY:
				return "Inventory";
			case OWNED_IN_BANK:
				return "Bank";
			case MISSING:
				return "Missing";
			case UNKNOWN:
			default:
				return "Unknown";
		}
	}

	private Component spacer(int height)
	{
		return Box.createVerticalStrut(height);
	}

	private void refreshUi()
	{
		revalidate();
		repaint();
	}

}
