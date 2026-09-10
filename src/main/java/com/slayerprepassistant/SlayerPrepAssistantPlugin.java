package com.slayerprepassistant;

import com.google.gson.Gson;
import com.google.inject.Provides;
import com.slayerprepassistant.bank.PlayerStateTracker;
import com.slayerprepassistant.gear.LoadoutMode;
import com.slayerprepassistant.guide.CombatMethod;
import com.slayerprepassistant.guide.MonsterGuide;
import com.slayerprepassistant.items.ItemResolver;
import com.slayerprepassistant.items.RuneLiteItemLookup;
import com.slayerprepassistant.prep.PreparationEngine;
import com.slayerprepassistant.prep.PreparationResult;
import com.slayerprepassistant.prep.PreparationStatus;
import com.slayerprepassistant.task.SlayerTaskContext;
import com.slayerprepassistant.task.SlayerTaskService;
import com.slayerprepassistant.task.TargetOption;
import com.slayerprepassistant.task.TaskTargetResolver;
import com.slayerprepassistant.ui.SlayerPrepAssistantPanel;
import com.slayerprepassistant.wiki.WikiClient;
import com.slayerprepassistant.wiki.WikiImageService;
import com.slayerprepassistant.wiki.WikiPageCandidates;
import com.slayerprepassistant.wiki.WikiParsingResult;
import com.slayerprepassistant.wiki.WikiSetupLoader;
import com.slayerprepassistant.wiki.WikiStrategyParser;
import com.slayerprepassistant.wiki.WikiTitles;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@PluginDescriptor(name = "Slayer Prep Assistant")
public class SlayerPrepAssistantPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(SlayerPrepAssistantPlugin.class);
	private static final String CONFIG_GROUP = "slayer-prep-assistant";
	private static final String SLAYER_GUIDE_MENU_OPTION = "Slayer guide";

	@Inject
	private Client client;

	@Inject
	private SlayerPrepAssistantConfig config;


	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OkHttpClient okHttpClient;

	@Inject
	private Gson gson;

	@Inject
	private ItemManager itemManager;

	@Inject
	private SpriteManager spriteManager;

	private final PlayerStateTracker playerStateTracker = new PlayerStateTracker();
	private final WikiStrategyParser wikiStrategyParser = new WikiStrategyParser();
	private final Map<String, String> preferredStrategyPageByTarget = new HashMap<>();
	private TaskTargetResolver targetResolver;
	private PreparationEngine preparationEngine;
	private SlayerTaskService slayerTaskService;
	private SlayerPrepAssistantPanel panel;
	private NavigationButton navigationButton;
	private SlayerTaskContext currentTask = SlayerTaskContext.none();
	private List<TargetOption> currentTargets = new ArrayList<>();
	private TargetOption selectedTarget;
	private WikiClient wikiClient;
	private WikiImageService wikiImageService;
	private WikiSetupLoader wikiSetupLoader;
	private ItemResolver itemResolver;
	private RuneLiteItemLookup itemLookup;
	private int wikiRequestId;
	private String lastTaskKey = "";
	private String lastTaskDisplayKey = "";
	private String lastPreparationRequestKey = "";
	private String visibleSetupTargetKey = "";

	@Override
	protected void startUp()
	{
		itemResolver = new ItemResolver();
		itemLookup = new RuneLiteItemLookup(itemManager);
		targetResolver = new TaskTargetResolver();
		preparationEngine = new PreparationEngine(itemResolver);
		slayerTaskService = new SlayerTaskService();
		wikiClient = new WikiClient(okHttpClient, gson);
		wikiImageService = new WikiImageService(wikiClient, config::useWikiStrategyData);
		wikiSetupLoader = new WikiSetupLoader(wikiClient, wikiStrategyParser, clientThread::invoke);
		panel = new SlayerPrepAssistantPanel(this::selectTarget, this::refreshPreparation, wikiImageService::load, itemManager, spriteManager, itemResolver, itemLookup, createIcon(32));
		navigationButton = NavigationButton.builder()
				.tooltip("Slayer Prep Assistant")
				.icon(createIcon(16))
				.priority(6)
				.panel(panel)
				.build();
		clientToolbar.addNavigation(navigationButton);
		refreshTask();
		log.debug("Slayer Prep Assistant started");
	}

	@Override
	protected void shutDown()
	{
		wikiRequestId++;
		if (wikiClient != null)
		{
			wikiClient.cancelOutstanding();
		}
		if (panel != null)
		{
			panel.dispose();
		}
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
		}
		preferredStrategyPageByTarget.clear();
		currentTargets = new ArrayList<>();
		selectedTarget = null;
		panel = null;
		navigationButton = null;
		wikiSetupLoader = null;
		wikiImageService = null;
		wikiClient = null;
		log.debug("Slayer Prep Assistant stopped");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event != null && (event.getGameState() == GameState.LOGGED_IN || event.getGameState() == GameState.LOGIN_SCREEN))
		{
			refreshTask();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event == null || (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM))
		{
			return;
		}
		if (slayerTaskService.applyChatMessage(Objects.requireNonNullElse(event.getMessage(), "")))
		{
			refreshTask();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event == null)
		{
			return;
		}
		int containerId = event.getContainerId();
		if (containerId != InventoryID.BANK && containerId != InventoryID.WORN && containerId != InventoryID.INV)
		{
			return;
		}
		if (containerId == InventoryID.BANK)
		{
			playerStateTracker.updateBank(event.getItemContainer(), client);
		}
		if (updatePlayerStateOnClientThread())
		{
			refreshPreparation();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event == null)
		{
			return;
		}
		String group = event.getGroup();
		if (CONFIG_GROUP.equals(group) || "slayer".equals(group))
		{
			lastPreparationRequestKey = "";
			if (CONFIG_GROUP.equals(group))
			{
				wikiRequestId++;
				loadTaskImage(selectedTarget);
				refreshPreparation();
				return;
			}
			refreshTask();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event == null)
		{
			return;
		}
		if (event.getVarpId() == VarPlayerID.SLAYER_COUNT
				|| event.getVarpId() == VarPlayerID.SLAYER_TARGET
				|| event.getVarpId() == VarPlayerID.SLAYER_AREA
				|| event.getVarpId() == VarPlayerID.SLAYER_COUNT_ORIGINAL
				|| event.getVarbitId() == VarbitID.SLAYER_TARGET_BOSSID
				|| event.getVarbitId() == VarbitID.SLAYER_MODIFIER_ID
				|| event.getVarbitId() == VarbitID.SLAYER_MODIFIER_VALUE
				|| event.getVarbitId() == VarbitID.SLAYER_MODIFIER_NEGATIVE)
		{
			refreshTaskOnClientThread();
		}
	}

	private void refreshTask()
	{
		clientThread.invokeLater(this::refreshTaskOnClientThread);
	}

	private void refreshTaskOnClientThread()
	{
		SlayerTaskContext nextTask = slayerTaskService.getCurrentTask(client);
		if (nextTask == null)
		{
			nextTask = SlayerTaskContext.none();
		}
		String nextTaskKey = taskKey(nextTask);
		String nextTaskDisplayKey = taskDisplayKey(nextTask);
		boolean taskChanged = !nextTaskKey.equals(lastTaskKey);
		boolean taskDisplayChanged = !nextTaskDisplayKey.equals(lastTaskDisplayKey);
		currentTask = nextTask;
		if (!currentTask.isActive())
		{
			if (taskDisplayChanged || selectedTarget != null || !currentTargets.isEmpty())
			{
				currentTargets = new ArrayList<>();
				selectedTarget = null;
				lastPreparationRequestKey = "";
				visibleSetupTargetKey = "";
				if (panel != null)
				{
					panel.showTask(currentTask, currentTargets);
					panel.showNoTask();
					panel.setTaskImage(null);
				}
			}
			lastTaskKey = nextTaskKey;
			lastTaskDisplayKey = nextTaskDisplayKey;
			return;
		}
		if (taskChanged || currentTargets.isEmpty())
		{
			List<TargetOption> resolvedTargets = targetResolver.resolve(currentTask);
			currentTargets = resolvedTargets == null ? new ArrayList<>() : new ArrayList<>(resolvedTargets);
			if (selectedTarget == null || !currentTargets.contains(selectedTarget))
			{
				selectedTarget = currentTargets.isEmpty() ? null : currentTargets.get(0);
			}
			lastPreparationRequestKey = "";
			visibleSetupTargetKey = "";
			if (panel != null)
			{
				panel.showTask(currentTask, currentTargets);
			}
			loadTaskImage(selectedTarget);
		}
		else if (taskDisplayChanged)
		{
			if (panel != null)
			{
				panel.showTask(currentTask, currentTargets);
			}
		}
		lastTaskKey = nextTaskKey;
		lastTaskDisplayKey = nextTaskDisplayKey;
		boolean playerStateChanged = updatePlayerStateOnClientThread();
		if (taskChanged || playerStateChanged)
		{
			refreshPreparation();
		}
	}

	private void selectTarget(TargetOption target)
	{
		selectedTarget = target;
		lastPreparationRequestKey = "";
		loadTaskImage(target);
		refreshPreparation();
	}

	private void refreshPreparation()
	{
		if (panel == null || selectedTarget == null)
		{
			if (panel != null && (currentTask == null || !currentTask.isActive()))
			{
				panel.showNoTask();
			}
			return;
		}
		CombatMethod method = panel.getSelectedMethod();
		LoadoutMode mode = panel.getSelectedLoadoutMode();
		String preparationRequestKey = preparationRequestKey(method, mode);
		if (preparationRequestKey.equals(lastPreparationRequestKey))
		{
			return;
		}
		lastPreparationRequestKey = preparationRequestKey;
		if (config == null || !config.useWikiStrategyData())
		{
			showPreparation(PreparationResult.noSetup(currentTask, currentTargets, selectedTarget, "No setup found."));
			return;
		}
		loadWikiStrategy(selectedTarget, method, mode);
	}

	private void showPreparation(PreparationResult result)
	{
		if (panel == null)
		{
			return;
		}
		if (result != null && result.getStatus() == PreparationStatus.SETUP_READY)
		{
			visibleSetupTargetKey = TargetOption.lookupKey(result.getSelectedTarget());
		}
		else
		{
			visibleSetupTargetKey = "";
		}
		panel.showPreparation(result);
	}

	private boolean hasVisibleSetupFor(TargetOption target)
	{
		return target != null && !visibleSetupTargetKey.isEmpty() && visibleSetupTargetKey.equals(TargetOption.lookupKey(target));
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (event == null || event.getMenuEntry() == null || event.getMenuEntry().getType() != MenuAction.EXAMINE_NPC)
		{
			return;
		}
		NPC npc = event.getMenuEntry().getNpc();
		if (!isGuideEligibleNpc(npc))
		{
			return;
		}
		if (client != null && client.getMenu() != null)
		{
			client.getMenu().createMenuEntry(-1)
					.setOption(SLAYER_GUIDE_MENU_OPTION)
					.setTarget(Objects.requireNonNullElse(event.getTarget(), ""))
					.setType(MenuAction.RUNELITE)
					.onClick(menuEntry -> openSlayerGuide(npc));
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event == null
			|| (event.getMenuAction() != MenuAction.CC_OP && event.getMenuAction() != MenuAction.CC_OP_LOW_PRIORITY)
			|| !"Check".equals(event.getMenuOption()))
		{
			return;
		}

		int itemId = ItemVariationMapping.map(event.getItemId());
		if (itemId == ItemID.SLAYER_HELM || itemId == ItemID.SLAYER_RING_8 || itemId == ItemID.SLAYER_GEM)
		{
			log.debug("Slayer task check item clicked");
			clientThread.invokeLater(this::refreshTaskOnClientThread);
		}
	}

	private boolean isGuideEligibleNpc(NPC npc)
	{
		if (npc == null)
		{
			return false;
		}
		NPCComposition composition = npc.getTransformedComposition();
		String name = composition == null ? npc.getName() : composition.getName();
		int combatLevel = composition == null ? npc.getCombatLevel() : composition.getCombatLevel();
		return name != null && !name.trim().isEmpty() && combatLevel > 0;
	}

	private void openSlayerGuide(NPC npc)
	{
		if (!isGuideEligibleNpc(npc))
		{
			return;
		}
		NPCComposition composition = npc.getTransformedComposition();
		String name = composition == null ? npc.getName() : composition.getName();
		TargetOption target = enemyTarget(name);
		currentTask = new SlayerTaskContext(target.getDisplayName(), 0, 0, "Clicked enemy", true);
		currentTargets = new ArrayList<>(Collections.singletonList(target));
		selectedTarget = target;
		lastTaskKey = taskKey(currentTask);
		lastTaskDisplayKey = taskDisplayKey(currentTask);
		lastPreparationRequestKey = "";
		visibleSetupTargetKey = "";
		SwingUtilities.invokeLater(() -> {
			if (clientToolbar != null && navigationButton != null)
			{
				clientToolbar.openPanel(navigationButton);
			}
		});
		updatePlayerStateOnClientThread();
		if (panel != null)
		{
			panel.showTask(currentTask, currentTargets);
		}
		loadTaskImage(selectedTarget);
		refreshPreparation();
	}

	private TargetOption enemyTarget(String npcName)
	{
		String safeName = Objects.requireNonNullElse(npcName, "");
		String title = WikiTitles.wikiTitle(safeName);
		return new TargetOption(title, title, "Strategies/" + title);
	}

	private void loadWikiStrategy(TargetOption target, CombatMethod method, LoadoutMode mode)
	{
		if (target == null || wikiSetupLoader == null)
		{
			return;
		}
		int requestId = ++wikiRequestId;
		if (!hasVisibleSetupFor(target) && panel != null)
		{
			panel.showLoading(currentTargets, target);
		}
		wikiSetupLoader.loadStrategy(
				requestId,
				target,
				strategyPageCandidates(target),
				this::isStaleWikiRequest,
				(parsed, finalCandidate) -> handleStrategyParse(requestId, target, method, mode, parsed, finalCandidate),
				() -> loadWikiVariants(requestId, target));
	}

	private boolean handleStrategyParse(int requestId, TargetOption target, CombatMethod method, LoadoutMode mode, WikiParsingResult parsed, boolean finalCandidate)
	{
		if (target == null || requestId != wikiRequestId || !target.equals(selectedTarget) || parsed == null || preparationEngine == null)
		{
			return true;
		}
		MonsterGuide guide = parsed.getGuide();
		if (guide == null)
		{
			return false;
		}
		PreparationResult wikiResult = preparationEngine.prepareGuide(currentTask, currentTargets, target, guide, method, mode, playerStateTracker.getState());
		if (wikiResult != null && wikiResult.getStatus() == PreparationStatus.SETUP_READY)
		{
			preferredStrategyPageByTarget.put(TargetOption.lookupKey(target), guide.getWikiTitle());
			showPreparation(wikiResult);
			return true;
		}
		return false;
	}

	private void loadWikiVariants(int requestId, TargetOption target)
	{
		if (target == null || wikiSetupLoader == null)
		{
			return;
		}
		wikiSetupLoader.loadVariantPage(
				requestId,
				target,
				WikiPageCandidates.variantPages(target),
				this::isStaleWikiRequest,
				parsed -> handleVariantsParse(requestId, target, parsed),
				() -> showPreparation(PreparationResult.noSetup(currentTask, currentTargets, target, "A usable strategy setup could not be found for this monster.")));
	}

	private boolean handleVariantsParse(int requestId, TargetOption target, WikiParsingResult parsed)
	{
		if (target == null || requestId != wikiRequestId || !target.equals(selectedTarget) || parsed == null || parsed.getVariants() == null)
		{
			return true;
		}
		List<TargetOption> variants = parsed.getVariants().stream()
				.filter(Objects::nonNull)
				.map(variant -> variant.toTargetOption())
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		if (variants.isEmpty())
		{
			return false;
		}
		if (!hasVisibleSetupFor(target) && panel != null)
		{
			panel.showLoading(currentTargets, target);
		}
		filterWikiVariants(requestId, target, variants, new ArrayList<>(), 0);
		return true;
	}

	private void filterWikiVariants(int requestId, TargetOption originalTarget, List<TargetOption> variants, List<TargetOption> variantsWithSetup, int index)
	{
		if (originalTarget == null || requestId != wikiRequestId || !originalTarget.equals(selectedTarget) || variants == null || variantsWithSetup == null)
		{
			return;
		}
		if (index >= variants.size())
		{
			showFilteredVariants(originalTarget, variantsWithSetup);
			return;
		}

		TargetOption variant = variants.get(index);
		checkVariantSetup(requestId, originalTarget, variant, hasSetup ->
		{
			if (hasSetup)
			{
				variantsWithSetup.add(variant);
			}
			filterWikiVariants(requestId, originalTarget, variants, variantsWithSetup, index + 1);
		});
	}

	private void showFilteredVariants(TargetOption originalTarget, List<TargetOption> variantsWithSetup)
	{
		if (originalTarget == null || panel == null)
		{
			return;
		}
		if (variantsWithSetup == null || variantsWithSetup.isEmpty())
		{
			showPreparation(PreparationResult.noSetup(currentTask, currentTargets, originalTarget, "A usable strategy setup could not be found for any monster variant."));
			return;
		}
		currentTargets = variantsWithSetup;
		selectedTarget = null;
		panel.showTask(currentTask, currentTargets);
		panel.showVariantSelection(currentTask, currentTargets, "No direct setup found. Choose a monster variant.");
	}

	private void checkVariantSetup(int requestId, TargetOption originalTarget, TargetOption variant, Consumer<Boolean> resultConsumer)
	{
		if (originalTarget == null || requestId != wikiRequestId || !originalTarget.equals(selectedTarget) || resultConsumer == null)
		{
			return;
		}
		if (wikiSetupLoader == null)
		{
			resultConsumer.accept(false);
			return;
		}

		wikiSetupLoader.loadStrategy(
			requestId,
			originalTarget,
			variant,
			strategyPageCandidates(variant),
			this::isStaleWikiRequest,
			(parsed, finalCandidate) ->
			{
				if (hasUsableSetup(variant, parsed))
				{
					resultConsumer.accept(true);
					return true;
				}
				return false;
			},
			() -> resultConsumer.accept(false));
	}

	private boolean isStaleWikiRequest(int requestId, TargetOption target)
	{
		return requestId != wikiRequestId || !Objects.equals(target, selectedTarget);
	}

	private boolean hasUsableSetup(TargetOption target, WikiParsingResult parsed)
	{
		if (target == null || parsed == null || parsed.getGuide() == null || preparationEngine == null)
		{
			return false;
		}
		PreparationResult result = preparationEngine.prepareGuide(currentTask, currentTargets, target, parsed.getGuide(), CombatMethod.defaultMethod(), LoadoutMode.BEST_I_OWN, playerStateTracker.getState());
		return result != null && result.getStatus() == PreparationStatus.SETUP_READY;
	}

	private boolean updatePlayerStateOnClientThread()
	{
		return playerStateTracker != null && playerStateTracker.update(client);
	}

	private String preparationRequestKey(CombatMethod method, LoadoutMode mode)
	{
		return taskKey(currentTask)
				+ "|" + TargetOption.lookupKey(selectedTarget)
				+ "|" + method
				+ "|" + mode
				+ "|wiki=" + (config != null && config.useWikiStrategyData())
				+ "|" + (playerStateTracker != null ? playerStateTracker.getStateKey() : "");
	}

	private String taskKey(SlayerTaskContext task)
	{
		if (task == null)
		{
			return "";
		}
		return task.isActive()
				+ "|" + Objects.requireNonNullElse(task.getTaskName(), "");
	}

	private String taskDisplayKey(SlayerTaskContext task)
	{
		if (task == null)
		{
			return "";
		}
		return task.isActive()
				+ "|" + Objects.requireNonNullElse(task.getTaskName(), "")
				+ "|" + task.getRemainingAmount()
				+ "|" + task.getInitialAmount()
				+ "|" + Objects.requireNonNullElse(task.getAssignedLocation(), "");
	}

	private List<String> strategyPageCandidates(TargetOption target)
	{
		if (target == null)
		{
			return Collections.emptyList();
		}
		return WikiPageCandidates.strategyPages(target, preferredStrategyPageByTarget.get(TargetOption.lookupKey(target)));
	}

	private void loadTaskImage(TargetOption target)
	{
		if (target == null || config == null || !config.useWikiStrategyData() || panel == null || wikiImageService == null)
		{
			if (panel != null)
			{
				panel.setTaskImage(null);
			}
			return;
		}
		String title = target.getWikiPage();
		if (title == null || title.trim().isEmpty())
		{
			panel.setTaskImage(null);
			return;
		}
		wikiImageService.load(title, 48, image ->
		{
			if (clientThread != null)
			{
				clientThread.invoke(() ->
				{
					if (config != null && config.useWikiStrategyData() && target.equals(selectedTarget) && panel != null)
					{
						panel.setTaskImage(image);
					}
				});
			}
		});
	}

	private BufferedImage createIcon(int size)
	{
		try (InputStream stream = SlayerPrepAssistantPlugin.class.getResourceAsStream("/slayer-prep-assistant-icon.png"))
		{
			if (stream != null)
			{
				BufferedImage img = ImageIO.read(stream);
				if (img != null)
				{
					return ImageUtil.resizeImage(img, size, size);
				}
			}
		}
		catch (IOException ex)
		{
			log.debug("Unable to load Slayer Prep Assistant icon", ex);
		}
		return new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
	}

	@Provides
	SlayerPrepAssistantConfig provideConfig(ConfigManager configManager)
	{
		return configManager != null ? configManager.getConfig(SlayerPrepAssistantConfig.class) : null;
	}
}
