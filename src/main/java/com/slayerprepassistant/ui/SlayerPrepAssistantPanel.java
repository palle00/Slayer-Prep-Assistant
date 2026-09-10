package com.slayerprepassistant.ui;

import com.slayerprepassistant.bank.OwnershipState;
import com.slayerprepassistant.gear.GearMatch;
import com.slayerprepassistant.gear.GearMatcher;
import com.slayerprepassistant.gear.GearRecommendation;
import com.slayerprepassistant.gear.GearSlot;
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
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
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
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import net.runelite.api.Skill;
import net.runelite.api.gameval.SpriteID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

public class SlayerPrepAssistantPanel extends PluginPanel
{
	static final Color BACKGROUND = new Color(46, 46, 46);
	static final Color PANEL = new Color(34, 34, 34);
	static final Color PANEL_LIGHT = new Color(24, 24, 24);
	static final Color BORDER = new Color(60, 60, 60);
	static final Color TEXT = new Color(230, 230, 230);
	static final Color MUTED = new Color(168, 168, 168);
	static final Color GOLD = new Color(255, 176, 0);
	static final Color GREEN = new Color(128, 206, 82);
	static final Color BLUE = new Color(82, 168, 214);
	static final Color YELLOW = new Color(255, 207, 38);
	static final Color RED = new Color(221, 83, 72);
	private static final Color CONTROL_BASE = new Color(24, 24, 24);
	private static final Color CONTROL_HOVER = new Color(34, 34, 34);
	private static final Color CONTROL_PRESSED = new Color(18, 18, 18);
	private static final Color CONTROL_SELECTED = new Color(48, 39, 20);
	private static final Color BORDER_HOVER = new Color(118, 118, 118);
	private static final int SIDEBAR_WIDTH = 226;
	private static final int CONTROL_HEIGHT = 34;
	private static final int WRAP_WIDTH = 166;
	private static final int TASK_TEXT_WIDTH = 112;
	private static final int GEAR_TEXT_WIDTH = 74;
	private static final int ALTERNATIVE_TEXT_WIDTH = 68;
	private static final int INVENTORY_ROW_HEIGHT = 34;
	private static final int INVENTORY_MAX_VISIBLE_ROWS = 8;
	private static final String COMBAT_ICON = "/asset-combat.png";
	private static final String INVENTORY_ICON = "/asset-inventory.png";
	private static final String WIKI_ICON = "/asset-wiki.png";
	static final int CARD_RADIUS = 3;
	static final int CONTROL_RADIUS = 3;
	static final float FONT_XS = 13f;
	static final float FONT_SM = 14f;
	private static final float FONT_MD = 15f;
	static final float FONT_XL = 17f;
	private static final float FONT_TITLE = 18f;
	private static final float FONT_TASK_TITLE = 19f;

	private final java.util.function.Consumer<TargetOption> targetConsumer;
	private final Runnable refreshConsumer;
	private final WikiImageLoader wikiImageLoader;
	private final ItemManager itemManager;
	private final SpriteManager spriteManager;
	private final ItemResolver itemResolver;
	private final RuneLiteItemLookup itemLookup;
	private final GearMatcher gearMatcher;
	private final BufferedImage pluginIcon;
	private final SkillIconManager skillIconManager = new SkillIconManager();
	private final ImageIcon equipmentHeaderIcon = resourceIcon(COMBAT_ICON, 18);
	private final ImageIcon inventoryIcon = resourceIcon(INVENTORY_ICON, 18);
	private final ImageIcon wikiIcon = resourceIcon(WIKI_ICON, 16);

	private final JPanel pinnedPanel = verticalPanel(BACKGROUND);
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
	private final JButton openWikiButton = new JButton("Open Wiki Page");
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
	private String lastStateRenderKey = "";
	private BufferedImage displayedTaskImage;
	private int skeletonFrame;

	public SlayerPrepAssistantPanel(
			java.util.function.Consumer<TargetOption> targetConsumer,
			Runnable refreshConsumer,
			WikiImageLoader wikiImageLoader,
			ItemManager itemManager,
			SpriteManager spriteManager,
			ItemResolver itemResolver,
			RuneLiteItemLookup itemLookup,
			BufferedImage pluginIcon)
	{
		super(false);
		this.targetConsumer = targetConsumer;
		this.refreshConsumer = refreshConsumer;
		this.wikiImageLoader = wikiImageLoader;
		this.itemManager = itemManager;
		this.spriteManager = spriteManager;
		this.itemResolver = itemResolver;
		this.itemLookup = itemLookup;
		this.gearMatcher = new GearMatcher();
		this.pluginIcon = pluginIcon;
		this.skeletonTimer = new Timer(120, event ->
		{
			skeletonFrame = (skeletonFrame + 1) % 12;
			resultPanel.repaint();
		});
		setLayout(new BorderLayout());
		setBackground(BACKGROUND);
		setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));

		pinnedPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
		body.setBorder(BorderFactory.createEmptyBorder(5, 8, 8, 8));
		add(pinnedPanel, BorderLayout.NORTH);
		add(contentScrollPane(), BorderLayout.CENTER);
		add(footerPanel(), BorderLayout.SOUTH);
		buildBaseLayout();
		showNoTask();
	}

	public void dispose()
	{
		runOnEdt(this::stopSkeletonLoading);
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
		runOnEdt(() ->
		{
			boolean activeTask = taskContext != null && taskContext.isActive();
			String taskName = activeTask ? taskContext.getTaskName() : "No Slayer task detected";
			String taskMeta = activeTask ? remainingText(taskContext) : "Check your Slayer helmet or gem.";
			String taskRenderKey = activeTask + "|" + taskName + "|" + taskMeta + "|" + targetsKey(targets);
			if (!taskRenderKey.equals(lastTaskRenderKey))
			{
				taskStatusLabel.setText(activeTask ? "Task detected" : "No active task");
				taskStatusLabel.setForeground(activeTask ? GREEN : MUTED);
				taskNameLabel.setText(wrapHtml(taskName, TASK_TEXT_WIDTH, false));
				taskMetaLabel.setText(wrapHtml(taskMeta, TASK_TEXT_WIDTH, false));
				lastTaskRenderKey = taskRenderKey;
			}
			if (taskCardPanel != null)
			{
				taskCardPanel.setVisible(activeTask);
				fitHeight(pinnedPanel);
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
		runOnEdt(() ->
		{
			if (displayedTaskImage == image)
			{
				return;
			}
			displayedTaskImage = image;
			taskIconLabel.setIcon(new ImageIcon(fitImage(image == null ? pluginIcon : image, 42, 42)));
			refreshUi();
		});
	}

	public void showLoading(SlayerTaskContext taskContext, List<TargetOption> targets, TargetOption target)
	{
		runOnEdt(() ->
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
		runOnEdt(() ->
		{
			stopSkeletonLoading();
			selectedTarget = null;
			currentResult = PreparationResult.noSetup(taskContext, variants, null, message);
			lastResultRenderKey = "";
			rebuildResult();
		});
	}

	public void showPreparation(PreparationResult result)
	{
		runOnEdt(() ->
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
		pinnedPanel.removeAll();
		pinnedPanel.add(headerPanel());
		pinnedPanel.add(spacer(5));
		taskCardPanel = taskCard();
		pinnedPanel.add(taskCardPanel);
		fitHeight(pinnedPanel);

		body.removeAll();
		body.add(controlsPanel());
		body.add(spacer(5));
		body.add(resultPanel);
	}

	private JScrollPane contentScrollPane()
	{
		JScrollPane scrollPane = new JScrollPane(body);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.setBackground(BACKGROUND);
		scrollPane.getViewport().setBackground(BACKGROUND);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

		JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
		verticalBar.setUnitIncrement(16);
		verticalBar.setBlockIncrement(96);
		return scrollPane;
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
		taskIconLabel.setIcon(new ImageIcon(fitImage(pluginIcon, 42, 42)));
		taskIconLabel.setHorizontalAlignment(SwingConstants.CENTER);
		taskIconLabel.setPreferredSize(new Dimension(48, 48));
		taskIconLabel.setBorder(BorderFactory.createLineBorder(new Color(48, 48, 48)));
		row.add(taskIconLabel, BorderLayout.WEST);

		JPanel text = verticalPanel(PANEL);
		JLabel caption = label("Current Task", MUTED, Font.PLAIN, FONT_XS);
		taskNameLabel.setForeground(TEXT);
		taskNameLabel.setFont(taskNameLabel.getFont().deriveFont(Font.BOLD, FONT_TASK_TITLE));
		taskNameLabel.setVerticalAlignment(SwingConstants.TOP);
		taskMetaLabel.setForeground(MUTED);
		taskMetaLabel.setFont(taskMetaLabel.getFont().deriveFont(Font.PLAIN, FONT_SM));
		taskMetaLabel.setVerticalAlignment(SwingConstants.TOP);
		text.add(caption);
		text.add(taskNameLabel);
		text.add(taskMetaLabel);
		row.add(text, BorderLayout.CENTER);

		panel.add(row, BorderLayout.CENTER);
		fitHeight(panel);
		return panel;
	}

	private JPanel controlsPanel()
	{
		controlsPanel.removeAll();
		controlsPanel.setBorder(compoundBorder());
		targetBlock = controlRow(targetControlPanel());
		methodBlock = controlRow(methodPanel);
		loadoutBlock = controlRow(loadoutPanel);
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
				TargetOption target = (TargetOption) targetSelect.getSelectedItem();
				selectedTarget = target;
				currentWikiUrl = wikiUrl(target);
				targetConsumer.accept(target);
			}
		});
		panel.add(targetLabel, "label");
		panel.add(targetSelect, "select");
		return panel;
	}

	private JPanel controlRow(Component component)
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
		openWikiButton.setIcon(wikiIcon);
		openWikiButton.setIconTextGap(6);
		openWikiButton.setPreferredSize(new Dimension(1, CONTROL_HEIGHT));
		openWikiButton.addActionListener(event -> openCurrentWiki());
		applyButtonHover(openWikiButton, () -> false);
		footer.add(openWikiButton, BorderLayout.CENTER);
		return footer;
	}

	private void openCurrentWiki()
	{
		String url = safeText(currentWikiUrl);

		if (url.isEmpty() && currentResult != null && currentResult.getGuide() != null)
		{
			url = safeText(currentResult.getGuide().getWikiUrl());
		}
		if (url.isEmpty() && selectedTarget != null)
		{
			url = wikiUrl(selectedTarget);
		}

		Object dropdownTarget = targetSelect.getSelectedItem();
		if (url.isEmpty() && dropdownTarget instanceof TargetOption)
		{
			url = wikiUrl((TargetOption) dropdownTarget);
		}

		if (!url.isEmpty())
		{
			LinkBrowser.browse(url);
		}
	}

	public void showNoTask()
	{
		runOnEdt(this::showNoTaskNow);
	}

	private void showNoTaskNow()
	{
		stopSkeletonLoading();
		currentResult = null;
		selectedTarget = null;
		currentWikiUrl = "";
		lastResultRenderKey = "";
		lastTargetRenderKey = "";
		rebuildTargetControl(Collections.emptyList(), false);
		showSetupControls(false);
		if (taskCardPanel != null)
		{
			taskCardPanel.setVisible(false);
			fitHeight(pinnedPanel);
		}
		controlsPanel.setVisible(false);
		rebuildStatePanel("NO TASK", "Check your Slayer helmet or gem to load a task.", "The checklist will appear here once a task is detected.");
	}

	private void rebuildResult()
	{
		stopSkeletonLoading();
		lastStateRenderKey = "";
		resultPanel.removeAll();
		if (currentResult == null)
		{
			showSetupControls(false);
			rebuildStatePanel("NO SETUP", "Select a target to build a checklist.", null);
			return;
		}
		if (currentResult.getStatus() == PreparationStatus.NO_SETUP)
		{
			List<TargetOption> targets = currentResult.getTargets() == null ? Collections.emptyList() : currentResult.getTargets();
			rebuildTargetControl(targets, targets.size() > 1 && selectedTarget == null);
			showSetupControls(false);
			rebuildNoSetupPanel();
			return;
		}

		currentWikiUrl = currentResult.getGuide() == null ? "" : safeText(currentResult.getGuide().getWikiUrl());
		showTargetControl(true);
		showSetupControls(true);
		updateMethodChoices();
		resultPanel.add(equipmentSection());
		resultPanel.add(spacer(5));
		resultPanel.add(inventorySection());
		resultPanel.add(spacer(5));
		resultPanel.add(statusLegend());
		refreshUi();
	}

	private void rebuildStatePanel(String title, String message, String detail)
	{
		stopSkeletonLoading();
		currentWikiUrl = selectedTarget == null ? "" : wikiUrl(selectedTarget);
		String stateRenderKey = (title == null ? "" : title) + "|" + (message == null ? "" : message) + "|" + (detail == null ? "" : detail);
		if (stateRenderKey.equals(lastStateRenderKey) && resultPanel.getComponentCount() == 1)
		{
			return;
		}
		lastStateRenderKey = stateRenderKey;
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
		lastStateRenderKey = "";
		currentWikiUrl = selectedTarget == null ? "" : wikiUrl(selectedTarget);
		resultPanel.removeAll();

		JPanel state = card(new BorderLayout(0, 8));
		state.setBorder(BorderFactory.createCompoundBorder(
				roundedBorder(new Color(70, 70, 70), CARD_RADIUS),
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

		refreshUi();
	}

	private void rebuildSkeletonLoading()
	{
		lastStateRenderKey = "";
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

	private void openItemWiki(RecommendedItem item)
	{
		if (item == null || item.getName() == null || item.getName().trim().isEmpty())
		{
			return;
		}
		LinkBrowser.browse(WikiTitles.pageUrl(item.getName()));
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
			builder.append(target == null ? "" : safeText(TargetOption.lookupKey(target)));
		}
		return builder.toString();
	}


	private TargetOption matchingTarget(List<TargetOption> targets, TargetOption target)
	{
		if (target == null || targets == null || targets.isEmpty())
		{
			return null;
		}

		String wantedKey = safeText(TargetOption.lookupKey(target));
		for (TargetOption candidate : targets)
		{
			if (candidate != null && safeText(TargetOption.lookupKey(candidate)).equals(wantedKey))
			{
				return candidate;
			}
		}
		return null;
	}

	private String resultKey(PreparationResult result)
	{
		if (result == null)
		{
			return "";
		}

		StringBuilder builder = new StringBuilder();
		builder.append(result.getStatus()).append('|')
				.append(safeText(result.getMessage())).append('|')
				.append(TargetOption.lookupKey(result.getSelectedTarget())).append('|')
				.append(result.getSelectedMethod()).append('|')
				.append(selectedLoadoutMode);

		if (result.getGuide() != null)
		{
			builder.append("|wiki=").append(safeText(result.getGuide().getWikiUrl()));
			if (result.getGuide().getMethods() != null)
			{
				result.getGuide().getMethods().forEach(method ->
						builder.append("|method=").append(method == null ? "" : method.getMethod()));
			}
		}

		if (result.getReadinessResult() != null)
		{
			builder.append("|ready=").append(result.getReadinessResult().getPercentage());
		}

		if (result.getLoadoutResult() != null)
		{
			builder.append("|loadout=").append(result.getLoadoutResult().getMode());
			List<GearMatch> matches = result.getLoadoutResult().getGearMatches();
			if (matches != null)
			{
				for (GearMatch match : matches)
				{
					if (match == null)
					{
						builder.append("|gear=null");
						continue;
					}
					RecommendedItem item = match.getItem();
					builder.append("|gear=")
							.append(match.getSlot()).append(':')
							.append(item == null ? "" : safeText(item.getName())).append(':')
							.append(match.getOwnershipState()).append(':')
							.append(match.getTierPriority());
				}
			}
		}

		if (result.getStrategyMethod() != null)
		{
			List<InventoryRecommendation> inventory = result.getStrategyMethod().getInventory();
			if (inventory != null)
			{
				for (InventoryRecommendation recommendation : inventory)
				{
					if (recommendation == null)
					{
						builder.append("|inv=null");
						continue;
					}

					OwnershipState ownership = result.getPlayerState() == null
							? OwnershipState.UNKNOWN
							: inventoryOwnership(recommendation, result.getPlayerState());

					builder.append("|inv=")
							.append(safeText(recommendation.getItemOrCategory())).append(':')
							.append(recommendation.getMinimumQuantity()).append(':')
							.append(ownership);
				}
			}

			List<GearRecommendation> gear = result.getStrategyMethod().getGear();
			if (gear != null)
			{
				for (GearRecommendation recommendation : gear)
				{
					if (recommendation == null)
					{
						continue;
					}
					builder.append("|gearRec=").append(recommendation.getSlot());
					List<GearTier> tiers = recommendation.getTiers();
					if (tiers == null)
					{
						continue;
					}
					for (GearTier tier : tiers)
					{
						if (tier == null || tier.getAlternatives() == null)
						{
							continue;
						}
						for (RecommendedItem item : tier.getAlternatives())
						{
							builder.append(':').append(item == null ? "" : itemKey(item));
						}
					}
				}
			}
		}

		return builder.toString();
	}

	private JPanel statusLegend()
	{
		JPanel panel = new JPanel(new GridLayout(0, 2, 3, 3));
		panel.setBackground(BACKGROUND);
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.add(statusChip("Equipped", OwnershipState.EQUIPPED));
		panel.add(statusChip("Inventory", OwnershipState.OWNED_IN_INVENTORY));
		panel.add(statusChip("Bank", OwnershipState.OWNED_IN_BANK));
		panel.add(statusChip("Missing", OwnershipState.MISSING));
		panel.add(statusChip("Unknown", OwnershipState.UNKNOWN));
		fitHeight(panel);
		return panel;
	}

	private JPanel statusChip(String text, OwnershipState state)
	{
		return statusTextLabel(text, state, 88);
	}


	private String sectionSummary(int ready, int total)
	{
		return Math.max(0, ready) + "/" + Math.max(0, total) + " ready";
	}

	private int readyCount(List<GearMatch> matches)
	{
		if (matches == null)
		{
			return 0;
		}
		int ready = 0;
		for (GearMatch match : matches)
		{
			if (match != null && isReady(match.getOwnershipState()))
			{
				ready++;
			}
		}
		return ready;
	}

	private int readyInventoryCount(List<InventoryRecommendation> recommendations)
	{
		if (recommendations == null)
		{
			return 0;
		}
		int ready = 0;
		for (InventoryRecommendation recommendation : recommendations)
		{
			if (isReady(inventoryOwnership(recommendation)))
			{
				ready++;
			}
		}
		return ready;
	}

	private boolean isReady(OwnershipState state)
	{
		return state == OwnershipState.EQUIPPED || state == OwnershipState.OWNED_IN_INVENTORY || state == OwnershipState.OWNED_IN_BANK;
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
		List<GearMatch> displayMatches = new ArrayList<>();
		if (matches != null)
		{
			for (GearMatch match : matches)
			{
				if (match != null)
				{
					displayMatches.add(match);
				}
			}
		}
		if (displayMatches.isEmpty())
		{
			rows.add(wrapped("No equipment parsed.", MUTED));
		}
		for (int i = 0; i < displayMatches.size(); i++)
		{
			GearMatch match = displayMatches.get(i);
			if (selectedLoadoutMode == LoadoutMode.BEST_I_OWN && !isReady(match.getOwnershipState()))
			{
				rows.add(emptyGearRow(match.getSlot(), i));
			}
			else
			{
				rows.add(gearRow(match, i));
			}
		}
		return new CollapsibleSection("EQUIPMENT", sectionSummary(readyCount(displayMatches), displayMatches.size()), equipmentHeaderIcon, true, rows, this::refreshUi);
	}

	private JPanel emptyGearRow(GearSlot slot, int index)
	{
		Color rowBackground = index % 2 == 0 ? new Color(29, 29, 29) : new Color(24, 24, 24);
		JPanel wrapper = verticalPanel(rowBackground);
		wrapper.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
				BorderFactory.createEmptyBorder(0, 3, 0, 0)));

		JPanel row = new JPanel(new BorderLayout(7, 0));
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.setBackground(rowBackground);
		row.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
		row.add(emptySlotIcon(slot, 30), BorderLayout.WEST);

		JPanel text = verticalPanel(rowBackground);
		text.add(label(slot.displayName().toUpperCase(), MUTED, Font.BOLD, FONT_XS));
		text.add(label("Empty", MUTED, Font.PLAIN, FONT_SM));
		row.add(text, BorderLayout.CENTER);

		JLabel status = label("", MUTED, Font.BOLD, FONT_XS);
		status.setPreferredSize(new Dimension(52, 20));
		row.add(status, BorderLayout.EAST);
		wrapper.add(row);
		return wrapper;
	}

	private JPanel gearRow(GearMatch match, int index)
	{
		boolean missing = match.getOwnershipState() == OwnershipState.MISSING;
		Color rowBackground = missing ? new Color(39, 25, 24) : index % 2 == 0 ? new Color(29, 29, 29) : new Color(24, 24, 24);

		JPanel wrapper = new JPanel();
		wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
		wrapper.setBackground(rowBackground);
		wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
		wrapper.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, missing ? 2 : 0, 1, 0, missing ? RED : BORDER),
				BorderFactory.createEmptyBorder(0, missing ? 1 : 0, 0, 0)));

		JPanel row = new JPanel(new BorderLayout(7, 0));
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.setBackground(rowBackground);
		row.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		row.add(itemIcon(match.getItem(), 30), BorderLayout.WEST);

		JPanel text = verticalPanel(rowBackground);
		text.add(label(match.getSlot().displayName().toUpperCase(Locale.ROOT), MUTED, Font.BOLD, FONT_XS));
		String itemName = match.getItem() == null ? "Unknown item" : safeText(match.getItem().getName());
		text.add(wrappedLabel(itemName, TEXT, Font.PLAIN, FONT_SM, GEAR_TEXT_WIDTH, false));
		row.add(text, BorderLayout.CENTER);

		JPanel status = statusLabel(match.getOwnershipState());
		status.setPreferredSize(new Dimension(52, 20));
		row.add(status, BorderLayout.EAST);

		wrapper.add(row);

		JPanel alternatives = alternativesPanel(match);
		alternatives.setAlignmentX(Component.LEFT_ALIGNMENT);
		alternatives.setVisible(false);
		wrapper.add(alternatives);

		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		row.setToolTipText("Click to show alternatives. Double-click to open item wiki.");
		Color hoverBackground = missing ? new Color(48, 30, 28) : new Color(37, 37, 37);
		Border normalBorder = wrapper.getBorder();
		Border hoverBorder = BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 2, 1, 0, GOLD),
				BorderFactory.createEmptyBorder(0, 0, 0, 0));

		Timer singleClickTimer = new Timer(220, event ->
		{
			alternatives.setVisible(!alternatives.isVisible());
			refreshUi();
		});
		singleClickTimer.setRepeats(false);

		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent event)
			{
				wrapper.setBorder(hoverBorder);
				setBackgroundRecursive(wrapper, hoverBackground);
				row.repaint();
			}

			@Override
			public void mouseExited(MouseEvent event)
			{
				wrapper.setBorder(normalBorder);
				setBackgroundRecursive(wrapper, rowBackground);
				alternatives.setBackground(PANEL_LIGHT);
				setBackgroundRecursive(alternatives, PANEL_LIGHT);
				row.repaint();
			}

			@Override
			public void mouseClicked(MouseEvent event)
			{
				if (!SwingUtilities.isLeftMouseButton(event))
				{
					return;
				}

				if (event.getClickCount() >= 2)
				{
					singleClickTimer.stop();
					openItemWiki(match.getItem());
					return;
				}

				if (event.getClickCount() == 1)
				{
					singleClickTimer.restart();
				}
			}
		});
		return wrapper;
	}

	private JPanel alternativesPanel(GearMatch match)
	{
		JPanel panel = verticalPanel(PANEL_LIGHT);
		panel.setBorder(BorderFactory.createEmptyBorder(4, 6, 6, 6));
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);

		Set<String> seenItems = new LinkedHashSet<>();
		String selectedItemKey = itemKey(match == null ? null : match.getItem());

		findRecommendation(match).ifPresent(recommendation ->
		{
			List<GearTier> tiers = recommendation.getTiers();
			if (tiers == null)
			{
				return;
			}

			for (GearTier tier : tiers)
			{
				if (tier == null || tier.getAlternatives() == null)
				{
					continue;
				}

				for (RecommendedItem item : tier.getAlternatives())
				{
					String key = itemKey(item);
					if (key.isEmpty() || key.equals(selectedItemKey) || !seenItems.add(key))
					{
						continue;
					}

					OwnershipState state = itemOwnership(item);
					if (selectedLoadoutMode == LoadoutMode.BEST_I_OWN && !isReady(state))
					{
						continue;
					}

					JPanel row = new JPanel(new BorderLayout(4, 0));
					row.setBackground(PANEL_LIGHT);
					row.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
					row.setAlignmentX(Component.LEFT_ALIGNMENT);

					JPanel itemPanel = new JPanel(new BorderLayout(4, 0));
					itemPanel.setBackground(PANEL_LIGHT);
					itemPanel.add(itemIcon(item, 18), BorderLayout.WEST);
					itemPanel.add(
							wrappedLabel(safeText(item.getName()), TEXT, Font.PLAIN, FONT_XS, ALTERNATIVE_TEXT_WIDTH, false),
							BorderLayout.CENTER);

					row.add(itemPanel, BorderLayout.CENTER);
					row.add(statusTextLabel(inventoryStatusText(state), state, 42), BorderLayout.EAST);

					row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
					row.setToolTipText("Double-click to open item wiki.");
					row.addMouseListener(new MouseAdapter()
					{
						@Override
						public void mouseClicked(MouseEvent event)
						{
							if (SwingUtilities.isLeftMouseButton(event) && event.getClickCount() >= 2)
							{
								openItemWiki(item);
							}
						}
					});

					panel.add(row);
				}
			}
		});

		if (panel.getComponentCount() == 0)
		{
			JPanel emptyRow = new JPanel(new BorderLayout());
			emptyRow.setBackground(PANEL_LIGHT);
			emptyRow.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
			emptyRow.add(label("No alternatives available.", MUTED, Font.PLAIN, FONT_XS), BorderLayout.CENTER);
			panel.add(emptyRow);
		}

		fitHeight(panel);
		return panel;
	}


	private Optional<GearRecommendation> findRecommendation(GearMatch match)
	{
		if (match == null || currentResult == null || currentResult.getStrategyMethod() == null
				|| currentResult.getStrategyMethod().getGear() == null)
		{
			return Optional.empty();
		}

		return currentResult.getStrategyMethod().getGear().stream()
				.filter(recommendation -> recommendation != null && recommendation.getSlot() == match.getSlot())
				.findFirst();
	}

	private JPanel inventorySection()
	{
		JPanel inventoryBody = verticalPanel(PANEL);
		JPanel rows = verticalPanel(PANEL_LIGHT);

		List<InventoryRecommendation> rawRecommendations = currentResult.getStrategyMethod() == null
				? Collections.emptyList()
				: currentResult.getStrategyMethod().getInventory();

		Map<String, InventoryRecommendation> merged = new LinkedHashMap<>();
		if (rawRecommendations != null)
		{
			for (InventoryRecommendation recommendation : rawRecommendations)
			{
				if (recommendation == null)
				{
					continue;
				}

				String key = normalizedKey(recommendation.getItemOrCategory());
				if (key.isEmpty())
				{
					continue;
				}

				InventoryRecommendation existing = merged.get(key);
				if (existing == null || recommendation.getMinimumQuantity() > existing.getMinimumQuantity())
				{
					merged.put(key, recommendation);
				}
			}
		}

		List<InventoryRecommendation> recommendations = new ArrayList<>(merged.values());
		int totalRecommendations = recommendations.size();
		int visibleCount = Math.min(28, totalRecommendations);
		List<InventoryRecommendation> visibleRecommendations = recommendations.subList(0, visibleCount);

		if (visibleRecommendations.isEmpty())
		{
			rows.add(inventoryEmptyRow());
		}
		else
		{
			for (int i = 0; i < visibleRecommendations.size(); i++)
			{
				rows.add(inventoryRow(visibleRecommendations.get(i), i));
			}
		}

		inventoryBody.add(inventoryRowsScroll(rows, Math.max(1, visibleRecommendations.size())));
		inventoryBody.add(spacer(5));

		String slotText = visibleCount + " / 28 recommended";
		if (totalRecommendations > 28)
		{
			slotText += " (" + (totalRecommendations - 28) + " overflow)";
		}
		inventoryBody.add(label(slotText, totalRecommendations > 28 ? RED : MUTED, Font.PLAIN, FONT_XS));
		fitHeight(inventoryBody);

		CollapsibleSection section = new CollapsibleSection(
				"INVENTORY",
				sectionSummary(readyInventoryCount(visibleRecommendations), visibleRecommendations.size()),
				inventoryIcon,
				true,
				inventoryBody,
				this::refreshUi);
		fitHeight(section);
		return section;
	}

	private JScrollPane inventoryRowsScroll(JPanel rows, int rowCount)
	{
		JScrollPane scrollPane = new JScrollPane(rows);
		scrollPane.setBorder(BorderFactory.createLineBorder(BORDER));
		scrollPane.setBackground(PANEL_LIGHT);
		scrollPane.getViewport().setBackground(PANEL_LIGHT);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(rowCount > INVENTORY_MAX_VISIBLE_ROWS
				? JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
				: JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		scrollPane.getVerticalScrollBar().setUnitIncrement(INVENTORY_ROW_HEIGHT);
		int visibleRows = Math.min(INVENTORY_MAX_VISIBLE_ROWS, Math.max(1, rowCount));
		int height = visibleRows * INVENTORY_ROW_HEIGHT + 2;
		Dimension size = new Dimension(1, height);
		scrollPane.setPreferredSize(size);
		scrollPane.setMinimumSize(size);
		scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
		return scrollPane;
	}

	private JPanel inventoryRow(InventoryRecommendation recommendation, int index)
	{
		OwnershipState state = inventoryOwnership(recommendation);
		String itemName = safeText(recommendation.getItemOrCategory());
		RecommendedItem item = itemResolver.resolve(itemName);
		Color rowBackground = index % 2 == 0 ? new Color(22, 22, 22) : new Color(18, 18, 18);
		if (state == OwnershipState.MISSING)
		{
			rowBackground = new Color(36, 22, 21);
		}

		JPanel row = new JPanel(new BorderLayout(8, 0));
		row.setBackground(rowBackground);
		row.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, state == OwnershipState.MISSING ? 2 : 0, 1, 0, state == OwnershipState.MISSING ? RED : BORDER),
				BorderFactory.createEmptyBorder(2, state == OwnershipState.MISSING ? 4 : 6, 2, 6)));
		row.setPreferredSize(new Dimension(1, INVENTORY_ROW_HEIGHT));
		row.setMinimumSize(new Dimension(1, INVENTORY_ROW_HEIGHT));
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, INVENTORY_ROW_HEIGHT));
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		row.setToolTipText(itemName + " - " + displayStatusText(state) + ". Double-click to open item wiki.");

		row.add(itemIcon(item, 28), BorderLayout.WEST);
		row.add(wrappedLabel(itemName, TEXT, Font.PLAIN, FONT_SM, 88, false), BorderLayout.CENTER);

		JPanel meta = new JPanel(new BorderLayout(5, 0));
		meta.setBackground(rowBackground);
		meta.add(label("x " + Math.max(1, recommendation.getMinimumQuantity()), MUTED, Font.PLAIN, FONT_XS), BorderLayout.WEST);
		meta.add(inventoryStateLabel(state), BorderLayout.CENTER);
		meta.add(label(">", MUTED, Font.BOLD, FONT_SM), BorderLayout.EAST);
		row.add(meta, BorderLayout.EAST);

		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent event)
			{
				if (event.getClickCount() >= 2)
				{
					openItemWiki(item);
				}
			}
		});
		return row;
	}

	private JPanel inventoryEmptyRow()
	{
		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(PANEL_LIGHT);
		row.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		row.setPreferredSize(new Dimension(1, INVENTORY_ROW_HEIGHT));
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, INVENTORY_ROW_HEIGHT));
		row.add(label("No inventory recommendations parsed.", MUTED, Font.PLAIN, FONT_XS), BorderLayout.CENTER);
		return row;
	}

	private JPanel inventoryStateLabel(OwnershipState state)
	{
		JPanel panel = new JPanel(new BorderLayout(3, 0));
		panel.setOpaque(false);
		panel.add(statusDot(state), BorderLayout.WEST);
		panel.add(label(inventoryStatusText(state), statusColor(state), Font.PLAIN, FONT_XS), BorderLayout.CENTER);
		return panel;
	}

	private JPanel statusTextLabel(String text, OwnershipState state, int width)
	{
		JPanel panel = new JPanel(new BorderLayout(3, 0));
		panel.setOpaque(false);
		panel.setToolTipText(displayStatusText(state));
		JLabel label = label(text, statusColor(state), Font.PLAIN, FONT_XS);
		panel.add(statusDot(state), BorderLayout.WEST);
		panel.add(label, BorderLayout.CENTER);
		panel.setPreferredSize(new Dimension(width, 16));
		panel.setMinimumSize(new Dimension(width, 16));
		panel.setMaximumSize(new Dimension(width, 16));
		return panel;
	}

	private JComponent statusDot(OwnershipState state)
	{
		Color color = statusColor(state);
		return new JComponent()
		{
			{
				setPreferredSize(new Dimension(10, 10));
				setMinimumSize(new Dimension(10, 10));
				setMaximumSize(new Dimension(10, 10));
			}

			@Override
			protected void paintComponent(java.awt.Graphics graphics)
			{
				super.paintComponent(graphics);
				Graphics2D g = (Graphics2D) graphics.create();
				try
				{
					g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					g.setColor(color);
					g.fillOval(1, Math.max(1, (getHeight() - 8) / 2), 8, 8);
				}
				finally
				{
					g.dispose();
				}
			}
		};
	}

	private String inventoryStatusText(OwnershipState state)
	{
		if (state == OwnershipState.UNKNOWN && shouldShowBankDataHint())
		{
			return "Bank?";
		}
		if (state == null)
		{
			return "?";
		}
		switch (state)
		{
			case EQUIPPED:
				return "Eqp";
			case OWNED_IN_INVENTORY:
				return "Inv";
			case OWNED_IN_BANK:
				return "Bank";
			case MISSING:
				return "Miss";
			case UNKNOWN:
			default:
				return "?";
		}
	}

	private void rebuildTargetControl(List<TargetOption> targets, boolean awaitSelection)
	{
		List<TargetOption> safeTargets = targets == null ? Collections.emptyList() : targets;
		TargetOption matchingSelectedTarget = matchingTarget(safeTargets, selectedTarget);
		if (matchingSelectedTarget != null)
		{
			selectedTarget = matchingSelectedTarget;
		}
		else if (selectedTarget != null)
		{
			selectedTarget = null;
			currentWikiUrl = "";
		}

		boolean noSetupState = currentResult != null && currentResult.getStatus() == PreparationStatus.NO_SETUP;
		String targetRenderKey = noSetupState + "|" + awaitSelection + "|" + TargetOption.lookupKey(selectedTarget) + "|" + targetsKey(safeTargets);
		boolean targetControlChanged = !targetRenderKey.equals(lastTargetRenderKey);
		if (targetControlChanged)
		{
			lastTargetRenderKey = targetRenderKey;
			rebuilding = true;
			targetSelect.removeAllItems();
			for (TargetOption target : safeTargets)
			{
				targetSelect.addItem(target);
			}
			boolean showDropdown = safeTargets.size() > 1 || awaitSelection || noSetupState;
			((CardLayout) targetCards.getLayout()).show(targetCards, showDropdown ? "select" : "label");
			if (showDropdown)
			{
				if (selectedTarget != null && safeTargets.contains(selectedTarget))
				{
					targetSelect.setSelectedItem(selectedTarget);
				}
				else
				{
					targetSelect.setSelectedIndex(-1);
				}
			}
			targetLabel.setText(safeTargets.isEmpty() ? "No target selected" : safeTargets.get(0).getDisplayName());
			rebuilding = false;
		}
		boolean selectableNoSetupTargets = noSetupState && !safeTargets.isEmpty();
		showTargetControl(!noSetupState || selectableNoSetupTargets);
		controlsPanel.setVisible(!safeTargets.isEmpty() && (!noSetupState || selectableNoSetupTargets));
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
		if (currentResult != null && currentResult.getGuide() != null && currentResult.getGuide().getMethods() != null)
		{
			currentResult.getGuide().getMethods().forEach(method ->
			{
				if (method != null && method.getMethod() != null && !methodChoices.contains(method.getMethod()))
				{
					methodChoices.add(method.getMethod());
				}
			});
		}

		CombatMethod resultMethod = currentResult == null ? null : currentResult.getSelectedMethod();
		if (resultMethod != null && methodChoices.contains(resultMethod))
		{
			selectedMethod = resultMethod;
		}
		else if (!methodChoices.isEmpty())
		{
			selectedMethod = methodChoices.get(0);
		}
		else
		{
			selectedMethod = resultMethod == null ? CombatMethod.defaultMethod() : resultMethod;
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
			CombatMethod method = methodChoices.isEmpty() ? selectedMethod : methodChoices.get(0);
			JLabel label = new JLabel(methodIcon(method));
			label.setHorizontalAlignment(SwingConstants.CENTER);
			label.setToolTipText(methodLabel(method));
			label.setBorder(BorderFactory.createCompoundBorder(
					roundedBorder(GOLD, CONTROL_RADIUS),
					BorderFactory.createEmptyBorder(3, 3, 3, 3)));
			methodPanel.add(label);
			methodPanel.revalidate();
			methodPanel.repaint();
			return;
		}
		for (CombatMethod method : methodChoices)
		{
			JButton button = styledButton(new JButton(methodIcon(method)));
			button.setText("");
			button.setToolTipText(methodLabel(method));
			button.setFont(button.getFont().deriveFont(Font.PLAIN, FONT_XS));
			applyButtonHover(button, () -> method == selectedMethod);
			button.addActionListener(event ->
			{
				selectedMethod = method;
				rebuildMethodPanel();
				refreshConsumer.run();
			});
			methodPanel.add(button);
		}
		methodPanel.revalidate();
		methodPanel.repaint();
	}

	private void rebuildLoadoutPanel()
	{
		loadoutPanel.removeAll();
		loadoutPanel.setBackground(PANEL);
		loadoutPanel.add(loadoutButton(LoadoutMode.BEST_I_OWN));
		loadoutPanel.add(loadoutButton(LoadoutMode.MAX));
		loadoutPanel.revalidate();
		loadoutPanel.repaint();
	}

	private JButton loadoutButton(LoadoutMode mode)
	{
		JButton button = styledButton(mode.getDisplayName());
		applyButtonHover(button, () -> mode == selectedLoadoutMode);
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
		if (taskContext == null)
		{
			return "Remaining count unknown";
		}

		String amount = taskContext.getRemainingAmount() > 0
				? taskContext.getRemainingAmount() + " remaining"
				: "Remaining count unknown";
		String location = safeText(taskContext.getAssignedLocation()).trim();
		return location.isEmpty() ? amount : amount + " at " + location;
	}

	private OwnershipState inventoryOwnership(InventoryRecommendation recommendation)
	{
		if (currentResult == null || currentResult.getPlayerState() == null)
		{
			return OwnershipState.UNKNOWN;
		}
		return inventoryOwnership(recommendation, currentResult.getPlayerState());
	}

	private OwnershipState inventoryOwnership(InventoryRecommendation recommendation, com.slayerprepassistant.bank.PlayerInventoryState playerState)
	{
		if (recommendation == null || playerState == null)
		{
			return OwnershipState.UNKNOWN;
		}
		return gearMatcher.ownershipForInventoryItem(
			itemResolver.resolve(safeText(recommendation.getItemOrCategory())),
			playerState,
			Math.max(1, recommendation.getMinimumQuantity()));
	}

	private OwnershipState itemOwnership(RecommendedItem item)
	{
		if (item == null || currentResult == null || currentResult.getPlayerState() == null)
		{
			return OwnershipState.UNKNOWN;
		}
		return gearMatcher.ownershipFor(item, currentResult.getPlayerState());
	}

	private boolean hasBankData()
	{
		return currentResult != null
				&& currentResult.getPlayerState() != null
				&& currentResult.getPlayerState().getBankSnapshot() != null
				&& currentResult.getPlayerState().getBankSnapshot().isKnown();
	}

	private boolean shouldShowBankDataHint()
	{
		return selectedLoadoutMode == LoadoutMode.BEST_I_OWN && !hasBankData();
	}

	private String displayStatusText(OwnershipState state)
	{
		return state == OwnershipState.UNKNOWN && shouldShowBankDataHint() ? "Bank?" : statusText(state);
	}

	private JPanel statusLabel(OwnershipState state)
	{
		return statusTextLabel(displayStatusText(state), state, 52);
	}

	private JLabel itemIcon(RecommendedItem item, int size)
	{
		Integer itemId = resolveItemId(item);
		if (itemId != null)
		{
			AsyncBufferedImage image = itemManager.getImage(itemId);
			if (image != null)
			{
				JLabel label = new JLabel();
				label.setHorizontalAlignment(SwingConstants.CENTER);
				label.setPreferredSize(new Dimension(size, size));
				label.setMinimumSize(new Dimension(size, size));
				label.setMaximumSize(new Dimension(size, size));
				image.onLoaded(() -> runOnEdt(() ->
				{
					label.setIcon(new ImageIcon(fitImage(image, size, size)));
					label.revalidate();
					label.repaint();
				}));
				return label;
			}
		}

		String itemName = item == null ? "" : safeText(item.getName()).trim();
		JLabel fallback = label(itemName.isEmpty() ? "" : itemName.substring(0, 1).toUpperCase(Locale.ROOT), MUTED, Font.BOLD, FONT_XS);
		fallback.setHorizontalAlignment(SwingConstants.CENTER);
		fallback.setPreferredSize(new Dimension(size, size));
		fallback.setBorder(BorderFactory.createLineBorder(BORDER));
		loadWikiItemImage(item, size, image ->
		{
			fallback.setIcon(new ImageIcon(fitImage(image, size, size)));
			fallback.setText("");
			fallback.setBorder(null);
			fallback.revalidate();
			fallback.repaint();
		});
		return fallback;
	}

	private JLabel emptySlotIcon(GearSlot slot, int size)
	{
		JLabel label = new JLabel();
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setPreferredSize(new Dimension(size, size));
		label.setMinimumSize(new Dimension(size, size));
		if (spriteManager != null)
		{
			spriteManager.addSpriteTo(label, wornIconSprite(slot), 0);
		}
		else
		{
			label.setText(" ");
			label.setBorder(BorderFactory.createLineBorder(BORDER));
		}
		return label;
	}

	private int wornIconSprite(GearSlot slot)
	{
		switch (slot)
		{
			case HEAD:
				return SpriteID.Wornicons.HEAD;
			case CAPE:
				return SpriteID.Wornicons.CAPE;
			case NECK:
				return SpriteID.Wornicons.NECK;
			case AMMO:
				return SpriteID.Wornicons.AMMUNITION;
			case WEAPON:
				return SpriteID.Wornicons.WEAPON;
			case BODY:
				return SpriteID.Wornicons.TORSO;
			case SHIELD:
				return SpriteID.Wornicons.SHIELD;
			case LEGS:
				return SpriteID.Wornicons.LEGS;
			case HANDS:
				return SpriteID.Wornicons.HANDS;
			case FEET:
				return SpriteID.Wornicons.FEET;
			case RING:
				return SpriteID.Wornicons.RING;
			default:
				return SpriteID.Wornicons.WEAPON;
		}
	}

	private Image itemImage(RecommendedItem item, int size)
	{
		Integer itemId = resolveItemId(item);
		return itemId == null ? null : itemManager.getImage(itemId);
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
				runOnEdt(() -> callback.accept(image));
			}
		});
	}

	private Integer resolveItemId(RecommendedItem item)
	{
		if (item == null || item.getName() == null || item.getName().trim().isEmpty())
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
		return label(wrapHtml(text, WRAP_WIDTH, false), color, Font.PLAIN, FONT_XS);
	}

	private JLabel centeredWrapped(String text, Color color)
	{
		JLabel label = wrappedLabel(text, color, Font.PLAIN, FONT_XS, WRAP_WIDTH, true);
		label.setVerticalAlignment(SwingConstants.TOP);
		return label;
	}

	private JLabel wrappedLabel(String text, Color color, int style, float size, int width, boolean centered)
	{
		JLabel label = label(wrapHtml(text, width, centered), color, style, size);
		label.setVerticalAlignment(SwingConstants.TOP);
		return label;
	}

	private String wrapHtml(String text, int width, boolean centered)
	{
		String align = centered ? "; text-align:center" : "";
		return "<html><body style='width:" + Math.max(1, width) + "px" + align + "'>" + escape(text) + "</body></html>";
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
		button.setBackground(CONTROL_BASE);
		button.setFont(button.getFont().deriveFont(Font.PLAIN, FONT_SM));
		button.setFocusPainted(false);
		button.setOpaque(true);
		button.setContentAreaFilled(true);
		button.setBorder(controlBorder(false));
		button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		return button;
	}

	private void applyButtonHover(JButton button, BooleanSupplier selectedSupplier)
	{
		if (button == null)
		{
			return;
		}
		applyButtonState(button, selectedSupplier, false, false);
		button.addMouseListener(new MouseAdapter()
		{
			private boolean hovering;
			private boolean pressing;

			@Override
			public void mouseEntered(MouseEvent event)
			{
				hovering = true;
				applyButtonState(button, selectedSupplier, true, pressing);
			}

			@Override
			public void mouseExited(MouseEvent event)
			{
				hovering = false;
				pressing = false;
				applyButtonState(button, selectedSupplier, false, false);
			}

			@Override
			public void mousePressed(MouseEvent event)
			{
				pressing = true;
				applyButtonState(button, selectedSupplier, hovering, true);
			}

			@Override
			public void mouseReleased(MouseEvent event)
			{
				pressing = false;
				applyButtonState(button, selectedSupplier, hovering, false);
			}
		});
	}

	private void applyButtonState(JButton button, BooleanSupplier selectedSupplier, boolean hover, boolean pressed)
	{
		boolean selected = selectedSupplier != null && selectedSupplier.getAsBoolean();
		button.setBackground(pressed ? CONTROL_PRESSED : selected ? CONTROL_SELECTED : hover ? CONTROL_HOVER : CONTROL_BASE);
		button.setForeground(selected || hover ? Color.WHITE : TEXT);
		button.setBorder(controlBorder(selected, hover || pressed));
		button.repaint();
	}

	private void styleCombo(JComboBox<?> comboBox)
	{
		comboBox.setForeground(TEXT);
		comboBox.setBackground(new Color(20, 20, 20));
		comboBox.setFont(comboBox.getFont().deriveFont(Font.PLAIN, FONT_SM));
		comboBox.setBorder(controlBorder(false));
		comboBox.setFocusable(false);
		comboBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		comboBox.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent event)
			{
				comboBox.setBorder(controlBorder(false, true));
			}

			@Override
			public void mouseExited(MouseEvent event)
			{
				comboBox.setBorder(controlBorder(false, false));
			}
		});
	}

	private Border controlBorder(boolean selected)
	{
		return controlBorder(selected, false);
	}

	private Border controlBorder(boolean selected, boolean hover)
	{
		Color borderColor = selected ? GOLD : hover ? BORDER_HOVER : BORDER;
		return BorderFactory.createCompoundBorder(
				roundedBorder(borderColor, CONTROL_RADIUS),
				BorderFactory.createEmptyBorder(4, 6, 4, 6));
	}

	private void setBackgroundRecursive(Component component, Color background)
	{
		if (component == null || background == null)
		{
			return;
		}
		component.setBackground(background);
		if (component instanceof Container)
		{
			for (Component child : ((Container) component).getComponents())
			{
				setBackgroundRecursive(child, background);
			}
		}
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

	private ImageIcon methodIcon(CombatMethod method)
	{
		Skill skill;
		switch (method)
		{
			case MELEE:
				skill = Skill.ATTACK;
				break;
			case RANGED:
				skill = Skill.RANGED;
				break;
			case MAGIC:
				skill = Skill.MAGIC;
				break;
			default:
				skill = Skill.ATTACK;
		}
		BufferedImage image = skillIconManager.getSkillImage(skill);
		return image == null ? null : new ImageIcon(fitImage(image, 22, 22));
	}

	private static BufferedImage fitImage(Image image, int maxWidth, int maxHeight)
	{
		if (image == null || maxWidth <= 0 || maxHeight <= 0)
		{
			return new BufferedImage(Math.max(1, maxWidth), Math.max(1, maxHeight), BufferedImage.TYPE_INT_ARGB);
		}
		int sourceWidth = image.getWidth(null);
		int sourceHeight = image.getHeight(null);
		if (sourceWidth <= 0 || sourceHeight <= 0)
		{
			return new BufferedImage(maxWidth, maxHeight, BufferedImage.TYPE_INT_ARGB);
		}

		double scale = Math.min(1.0, Math.min(maxWidth / (double) sourceWidth, maxHeight / (double) sourceHeight));
		int width = Math.max(1, (int) Math.round(sourceWidth * scale));
		int height = Math.max(1, (int) Math.round(sourceHeight * scale));
		BufferedImage fitted = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = fitted.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.drawImage(image, 0, 0, width, height, null);
		g.dispose();
		return fitted;
	}

	private static ImageIcon resourceIcon(String path, int size)
	{
		if (path == null || path.trim().isEmpty() || size <= 0)
		{
			return null;
		}
		try (InputStream stream = SlayerPrepAssistantPanel.class.getResourceAsStream(path))
		{
			if (stream == null)
			{
				return null;
			}
			BufferedImage image = javax.imageio.ImageIO.read(stream);
			if (image == null)
			{
				return null;
			}
			return new ImageIcon(ImageUtil.resizeImage(image, size, size));
		}
		catch (Exception ex)
		{
			return null;
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
		if (state == null)
		{
			return MUTED;
		}
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
		if (state == null)
		{
			return "Unknown";
		}
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


	private String itemKey(RecommendedItem item)
	{
		if (item == null)
		{
			return "";
		}

		String name = ItemResolver.canonicalKey(item.getName());
		if (!name.isEmpty())
		{
			return "name:" + name;
		}
		return item.getItemIds().stream()
			.filter(java.util.Objects::nonNull)
			.min(Integer::compareTo)
			.map(itemId -> "id:" + itemId)
			.orElse("");
	}

	private String normalizedKey(String value)
	{
		return safeText(value).trim().toLowerCase(Locale.ROOT);
	}

	private String safeText(String value)
	{
		return value == null ? "" : value;
	}

	private void runOnEdt(Runnable action)
	{
		if (action == null)
		{
			return;
		}

		if (SwingUtilities.isEventDispatchThread())
		{
			action.run();
		}
		else
		{
			SwingUtilities.invokeLater(action);
		}
	}

	private Component spacer(int height)
	{
		return Box.createVerticalStrut(height);
	}

	private void refreshUi()
	{
		fitHeight(pinnedPanel);
		if (controlsPanel.isVisible())
		{
			fitHeight(controlsPanel);
		}
		resultPanel.revalidate();
		body.revalidate();
		revalidate();
		repaint();
	}

}
