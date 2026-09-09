package com.slayerprepassistant;

import com.google.gson.Gson;
import com.google.inject.Provides;
import com.slayerprepassistant.bank.BankSnapshotService;
import com.slayerprepassistant.bank.PlayerInventoryState;
import com.slayerprepassistant.gear.LoadoutMode;
import com.slayerprepassistant.gear.RecommendedItem;
import com.slayerprepassistant.guide.CombatMethod;
import com.slayerprepassistant.guide.MonsterGuide;
import com.slayerprepassistant.items.ItemResolver;
import com.slayerprepassistant.prep.PreparationEngine;
import com.slayerprepassistant.prep.PreparationResult;
import com.slayerprepassistant.prep.PreparationStatus;
import com.slayerprepassistant.task.SlayerTaskContext;
import com.slayerprepassistant.task.SlayerTaskService;
import com.slayerprepassistant.task.TargetOption;
import com.slayerprepassistant.task.TaskResolutionOverrides;
import com.slayerprepassistant.task.TaskTargetResolver;
import com.slayerprepassistant.ui.SlayerPrepAssistantPanel;
import com.slayerprepassistant.wiki.WikiClient;
import com.slayerprepassistant.wiki.WikiParsingResult;
import com.slayerprepassistant.wiki.WikiSetupLoader;
import com.slayerprepassistant.wiki.WikiStrategyParser;
import com.slayerprepassistant.wiki.WikiTitles;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemVariationMapping;
import net.runelite.client.events.ConfigChanged;
import net.runelite.http.api.item.ItemPrice;
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
	private static final String THIRD_PARTY_WARNING_ACKNOWLEDGED_KEY = "thirdPartyWarningAcknowledged";
	private static final String THIRD_PARTY_WARNING = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers";
	private static final String SLAYER_GUIDE_MENU_OPTION = "Slayer guide";

	@Inject
	private Client client;

	@Inject
	private SlayerPrepAssistantConfig config;

	@Inject
	private ConfigManager configManager;

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

	private final BankSnapshotService bankSnapshotService = new BankSnapshotService();
	private final WikiStrategyParser wikiStrategyParser = new WikiStrategyParser();
	private final Map<String, BufferedImage> wikiImageCache = new HashMap<>();
	private final Map<String, String> preferredStrategyPageByTarget = new HashMap<>();
	private final Map<String, OptionalInt> priceCache = new HashMap<>();
	private TaskTargetResolver targetResolver;
	private PreparationEngine preparationEngine;
	private SlayerTaskService slayerTaskService;
	private SlayerPrepAssistantPanel panel;
	private NavigationButton navigationButton;
	private SlayerTaskContext currentTask = SlayerTaskContext.none();
	private List<TargetOption> currentTargets = new ArrayList<>();
	private TargetOption selectedTarget;
	private PlayerInventoryState playerState = PlayerInventoryState.unknownBank();
	private WikiClient wikiClient;
	private WikiSetupLoader wikiSetupLoader;
	private ItemResolver itemResolver;
	private int wikiRequestId;
	private String lastTaskKey = "";
	private String lastTaskDisplayKey = "";
	private String lastPlayerStateKey = "";
	private String lastPreparationRequestKey = "";
	private String visibleSetupTargetKey = "";

	@Override
	protected void startUp()
	{
		itemResolver = new ItemResolver();
		targetResolver = new TaskTargetResolver(new TaskResolutionOverrides());
		preparationEngine = new PreparationEngine(itemResolver, this::priceFor);
		slayerTaskService = new SlayerTaskService(configManager);
		wikiClient = new WikiClient(okHttpClient, gson);
		wikiSetupLoader = new WikiSetupLoader(wikiClient, wikiStrategyParser, clientThread::invoke);
		panel = new SlayerPrepAssistantPanel(this::selectTarget, this::refreshPreparation, this::loadWikiImage, itemManager, itemResolver, createIcon(32));
		navigationButton = NavigationButton.builder()
			.tooltip("Slayer Prep Assistant")
			.icon(createIcon(16))
			.priority(6)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navigationButton);
		showThirdPartyWarningIfNeeded();
		refreshTask();
		log.debug("Slayer Prep Assistant started");
	}

	@Override
	protected void shutDown()
	{
		if (panel != null)
		{
			panel.dispose();
		}
		if (navigationButton != null)
		{
			clientToolbar.removeNavigation(navigationButton);
		}
		log.debug("Slayer Prep Assistant stopped");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN || event.getGameState() == GameState.LOGIN_SCREEN)
		{
			refreshTask();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM)
		{
			return;
		}
		if (slayerTaskService.applyChatMessage(event.getMessage()))
		{
			refreshTask();
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		int containerId = event.getContainerId();
		if (containerId != InventoryID.BANK && containerId != InventoryID.WORN && containerId != InventoryID.INV)
		{
			return;
		}
		if (containerId == InventoryID.BANK)
		{
			bankSnapshotService.updateFromBankContainer(event.getItemContainer(), client);
		}
		if (updatePlayerStateOnClientThread())
		{
			refreshPreparation();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (CONFIG_GROUP.equals(event.getGroup()) || "slayer".equals(event.getGroup()))
		{
			lastPreparationRequestKey = "";
			if (CONFIG_GROUP.equals(event.getGroup()))
			{
				refreshPreparation();
				return;
			}
			refreshTask();
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
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
				panel.showTask(currentTask, currentTargets);
				panel.showNoTask();
				panel.setTaskImage(null);
			}
			lastTaskKey = nextTaskKey;
			lastTaskDisplayKey = nextTaskDisplayKey;
			return;
		}
		if (taskChanged || currentTargets.isEmpty())
		{
			currentTargets = targetResolver.resolve(currentTask);
			if (selectedTarget == null || !currentTargets.contains(selectedTarget))
			{
				selectedTarget = currentTargets.isEmpty() ? null : currentTargets.get(0);
			}
			lastPreparationRequestKey = "";
			visibleSetupTargetKey = "";
			panel.showTask(currentTask, currentTargets);
			loadTaskImage(selectedTarget);
		}
		else if (taskDisplayChanged)
		{
			panel.showTask(currentTask, currentTargets);
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

	private void showThirdPartyWarningIfNeeded()
	{
		if (config.thirdPartyWarningAcknowledged())
		{
			return;
		}
		SwingUtilities.invokeLater(() ->
		{
			JOptionPane.showMessageDialog(
				null,
				THIRD_PARTY_WARNING,
				"Slayer Prep Assistant",
				JOptionPane.WARNING_MESSAGE);
			configManager.setConfiguration(CONFIG_GROUP, THIRD_PARTY_WARNING_ACKNOWLEDGED_KEY, true);
		});
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
		PreparationResult result = preparationEngine.prepare(currentTask, currentTargets, selectedTarget, method, mode, playerState);
		if (result.getStatus() == PreparationStatus.SETUP_READY || !config.useWikiStrategyData())
		{
			showPreparation(result);
			return;
		}
		loadWikiStrategy(selectedTarget, method, mode);
	}

	private void showPreparation(PreparationResult result)
	{
		if (result != null && result.getStatus() == PreparationStatus.SETUP_READY)
		{
			visibleSetupTargetKey = targetKey(result.getSelectedTarget());
		}
		else
		{
			visibleSetupTargetKey = "";
		}
		panel.showPreparation(result);
	}

	private boolean hasVisibleSetupFor(TargetOption target)
	{
		return !visibleSetupTargetKey.isEmpty() && visibleSetupTargetKey.equals(targetKey(target));
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (event.getMenuEntry().getType() != MenuAction.EXAMINE_NPC)
		{
			return;
		}
		NPC npc = event.getMenuEntry().getNpc();
		if (!isGuideEligibleNpc(npc))
		{
			return;
		}
		client.createMenuEntry(-1)
			.setOption(SLAYER_GUIDE_MENU_OPTION)
			.setTarget(event.getTarget())
			.setType(MenuAction.RUNELITE)
			.onClick(menuEntry -> openSlayerGuide(npc));
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if ((event.getMenuAction() != MenuAction.CC_OP && event.getMenuAction() != MenuAction.CC_OP_LOW_PRIORITY)
			|| !"Check".equals(event.getMenuOption()))
		{
			return;
		}
		Widget widget = client.getWidget(event.getParam1());
		if (widget == null)
		{
			return;
		}
		if (event.getParam0() != -1)
		{
			widget = widget.getChild(event.getParam0());
			if (widget == null)
			{
				return;
			}
		}
		int itemId = widget.getItemId();
		for (Widget child : widget.getDynamicChildren())
		{
			if (itemId == -1)
			{
				itemId = child.getItemId();
			}
		}
		itemId = ItemVariationMapping.map(itemId);
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
		Integer combatLevel = composition == null ? npc.getCombatLevel() : composition.getCombatLevel();
		TargetOption target = enemyTarget(name, combatLevel);
		currentTask = new SlayerTaskContext(target.getDisplayName(), 0, 0, "Clicked enemy", true);
		currentTargets = new ArrayList<>(java.util.Collections.singletonList(target));
		selectedTarget = target;
		lastTaskKey = taskKey(currentTask);
		lastTaskDisplayKey = taskDisplayKey(currentTask);
		lastPreparationRequestKey = "";
		visibleSetupTargetKey = "";
		SwingUtilities.invokeLater(() -> clientToolbar.openPanel(navigationButton));
		updatePlayerStateOnClientThread();
		panel.showTask(currentTask, currentTargets);
		loadTaskImage(selectedTarget);
		refreshPreparation();
	}

	private TargetOption enemyTarget(String npcName, Integer combatLevel)
	{
		String title = WikiTitles.wikiTitle(npcName);
		return new TargetOption(title, npcName, title, "Strategies/" + title, combatLevel, null, "Wiki-derived enemy lookup", true);
	}

	private void loadWikiStrategy(TargetOption target, CombatMethod method, LoadoutMode mode)
	{
		int requestId = ++wikiRequestId;
		if (!hasVisibleSetupFor(target))
		{
			panel.showLoading(currentTask, currentTargets, target);
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
		if (requestId != wikiRequestId || !target.equals(selectedTarget))
		{
			return true;
		}
		MonsterGuide guide = parsed.getGuide();
		PreparationResult wikiResult = preparationEngine.prepareGuide(currentTask, currentTargets, target, guide, method, mode, playerState);
		if (wikiResult.getStatus() == PreparationStatus.SETUP_READY)
		{
			preferredStrategyPageByTarget.put(targetKey(target), parsed.getGuide().getWikiTitle());
			showPreparation(wikiResult);
			return true;
		}
		return false;
	}

	private void loadWikiVariants(int requestId, TargetOption target)
	{
		wikiSetupLoader.loadVariantPage(
			requestId,
			target,
			variantPageCandidates(target),
			this::isStaleWikiRequest,
			parsed -> handleVariantsParse(requestId, target, parsed),
			() -> showPreparation(PreparationResult.noSetup(currentTask, currentTargets, target, "A usable strategy setup could not be found for this monster.")));
	}

	private boolean handleVariantsParse(int requestId, TargetOption target, WikiParsingResult parsed)
	{
		if (requestId != wikiRequestId || !target.equals(selectedTarget))
		{
			return true;
		}
		List<TargetOption> variants = parsed.getVariants().stream()
			.map(variant -> variant.toTargetOption())
			.collect(Collectors.toList());
		if (variants.isEmpty())
		{
			return false;
		}
		if (!hasVisibleSetupFor(target))
		{
			panel.showLoading(currentTask, currentTargets, target);
		}
		filterWikiVariants(requestId, target, variants, new ArrayList<>(), 0);
		return true;
	}

	private void filterWikiVariants(int requestId, TargetOption originalTarget, List<TargetOption> variants, List<TargetOption> variantsWithSetup, int index)
	{
		if (requestId != wikiRequestId || !originalTarget.equals(selectedTarget))
		{
			return;
		}
		if (index >= variants.size())
		{
			showFilteredVariants(originalTarget, variantsWithSetup);
			return;
		}
		TargetOption variant = variants.get(index);
		checkVariantSetup(requestId, originalTarget, variant, strategyPageCandidates(variant), 0, hasSetup ->
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
		if (variantsWithSetup.isEmpty())
		{
			showPreparation(PreparationResult.noSetup(currentTask, currentTargets, originalTarget, "A usable strategy setup could not be found for any monster variant."));
			return;
		}
		currentTargets = variantsWithSetup;
		selectedTarget = null;
		panel.showTask(currentTask, currentTargets);
		panel.showVariantSelection(currentTask, currentTargets, "No direct setup found. Choose a monster variant.");
	}

	private void checkVariantSetup(int requestId, TargetOption originalTarget, TargetOption variant, List<String> candidates, int index, Consumer<Boolean> resultConsumer)
	{
		if (requestId != wikiRequestId || !originalTarget.equals(selectedTarget))
		{
			return;
		}
		if (index >= candidates.size())
		{
			resultConsumer.accept(false);
			return;
		}
		String strategyTitle = candidates.get(index);
		wikiSetupLoader.loadStrategyCandidate(requestId, originalTarget, variant, strategyTitle, this::isStaleWikiRequest, parsed ->
		{
			if (hasUsableSetup(variant, parsed))
			{
				resultConsumer.accept(true);
				return;
			}
			checkVariantSetup(requestId, originalTarget, variant, candidates, index + 1, resultConsumer);
		});
	}

	private boolean isStaleWikiRequest(int requestId, TargetOption target)
	{
		return requestId != wikiRequestId || !target.equals(selectedTarget);
	}

	private boolean hasUsableSetup(TargetOption target, WikiParsingResult parsed)
	{
		if (parsed == null)
		{
			return false;
		}
		PreparationResult result = preparationEngine.prepareGuide(currentTask, currentTargets, target, parsed.getGuide(), CombatMethod.GENERAL, LoadoutMode.BEST_I_OWN, playerState);
		return result.getStatus() == PreparationStatus.SETUP_READY;
	}

	private boolean updatePlayerStateOnClientThread()
	{
		ItemContainer equipment = client.getItemContainer(InventoryID.WORN);
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		PlayerInventoryState nextPlayerState = new PlayerInventoryState(
			BankSnapshotService.toQuantities(equipment),
			BankSnapshotService.toQuantities(inventory),
			BankSnapshotService.toNames(equipment, client),
			BankSnapshotService.toNames(inventory, client),
			bankSnapshotService.getSnapshot());
		String nextPlayerStateKey = playerStateKey(nextPlayerState);
		if (nextPlayerStateKey.equals(lastPlayerStateKey))
		{
			return false;
		}
		playerState = nextPlayerState;
		lastPlayerStateKey = nextPlayerStateKey;
		return true;
	}

	private String preparationRequestKey(CombatMethod method, LoadoutMode mode)
	{
		return taskKey(currentTask)
			+ "|" + targetKey(selectedTarget)
			+ "|" + method
			+ "|" + mode
			+ "|wiki=" + config.useWikiStrategyData()
			+ "|price=" + config.useWikiPriceData()
			+ "|" + lastPlayerStateKey;
	}

	private String taskKey(SlayerTaskContext task)
	{
		if (task == null)
		{
			return "";
		}
		return task.isActive()
			+ "|" + task.getTaskName();
	}

	private String taskDisplayKey(SlayerTaskContext task)
	{
		if (task == null)
		{
			return "";
		}
		return task.isActive()
			+ "|" + task.getTaskName()
			+ "|" + task.getRemainingAmount()
			+ "|" + task.getInitialAmount()
			+ "|" + task.getAssignedLocation();
	}

	private String targetKey(TargetOption target)
	{
		if (target == null)
		{
			return "";
		}
		return target.getDisplayName()
			+ "|" + target.getWikiPage()
			+ "|" + target.getStrategyPage();
	}

	private String playerStateKey(PlayerInventoryState state)
	{
		return Objects.toString(new TreeMap<>(state.getEquipment()))
			+ "|" + Objects.toString(new TreeMap<>(state.getInventory()))
			+ "|" + Objects.toString(new TreeSet<>(state.getEquipmentNames()))
			+ "|" + Objects.toString(new TreeSet<>(state.getInventoryNames()))
			+ "|bankKnown=" + state.getBankSnapshot().isKnown()
			+ "|" + Objects.toString(new TreeMap<>(state.getBankSnapshot().getQuantitiesById()))
			+ "|" + Objects.toString(new TreeSet<>(state.getBankSnapshot().getItemNames()));
	}

	private OptionalInt priceFor(RecommendedItem item)
	{
		if (!config.useWikiPriceData() || item == null || item.getName().trim().isEmpty())
		{
			return OptionalInt.empty();
		}
		String normalizedName = ItemResolver.normalize(item.getName());
		OptionalInt cached = priceCache.get(normalizedName);
		if (cached != null)
		{
			return cached;
		}
		try
		{
			for (ItemPrice itemPrice : itemManager.search(item.getName()))
			{
				if (ItemResolver.normalize(itemPrice.getName()).equals(normalizedName))
				{
					int price = itemPrice.getWikiPrice() > 0 ? itemPrice.getWikiPrice() : itemPrice.getPrice();
					OptionalInt result = price > 0 ? OptionalInt.of(price) : OptionalInt.empty();
					priceCache.put(normalizedName, result);
					return result;
				}
			}
		}
		catch (RuntimeException ex)
		{
			log.debug("Unable to resolve Wiki price for {}", item.getName(), ex);
		}
		priceCache.put(normalizedName, OptionalInt.empty());
		return OptionalInt.empty();
	}

	private List<String> strategyPageCandidates(TargetOption target)
	{
		Set<String> candidates = new LinkedHashSet<>();
		addStrategyCandidate(candidates, preferredStrategyPageByTarget.get(targetKey(target)));
		String wikiPage = target.getWikiPage();
		String singular = WikiTitles.singularTitle(wikiPage);
		addStrategyCandidate(candidates, target.getStrategyPage());
		addStrategyCandidate(candidates, "Strategies/" + wikiPage);
		addStrategyCandidate(candidates, wikiPage + "/Strategies");
		addStrategyCandidate(candidates, "Strategies/" + singular);
		addStrategyCandidate(candidates, singular + "/Strategies");
		return new ArrayList<>(candidates);
	}

	private void addStrategyCandidate(Set<String> candidates, String pageTitle)
	{
		if (pageTitle != null && !pageTitle.trim().isEmpty())
		{
			candidates.add(pageTitle.trim());
		}
	}

	private void loadTaskImage(TargetOption target)
	{
		if (target == null || !config.useWikiStrategyData())
		{
			panel.setTaskImage(null);
			return;
		}
		String title = target.getWikiPage();
		loadWikiImage(title, 48, image -> clientThread.invoke(() ->
		{
			if (target.equals(selectedTarget))
			{
				panel.setTaskImage(image);
			}
		}));
	}

	private void loadWikiImage(String title, int size, Consumer<BufferedImage> callback)
	{
		if (!config.useWikiStrategyData() || title == null || title.trim().isEmpty())
		{
			callback.accept(null);
			return;
		}
		String key = title.trim().toLowerCase(java.util.Locale.ROOT) + "|" + size;
		BufferedImage cached = wikiImageCache.get(key);
		if (cached != null)
		{
			callback.accept(cached);
			return;
		}
		wikiClient.fetchPageImage(title, size, image ->
		{
			if (image != null)
			{
				wikiImageCache.put(key, image);
			}
			callback.accept(image);
		});
	}

	private List<String> variantPageCandidates(TargetOption target)
	{
		Set<String> candidates = new LinkedHashSet<>();
		addVariantCandidates(candidates, target.getWikiPage());
		addVariantCandidates(candidates, WikiTitles.singularTitle(target.getWikiPage()));
		return new ArrayList<>(candidates);
	}

	private void addVariantCandidates(Set<String> candidates, String pageTitle)
	{
		if (pageTitle == null || pageTitle.trim().isEmpty())
		{
			return;
		}
		String cleaned = pageTitle.trim();
		candidates.add(cleaned);
		if (!cleaned.startsWith("Slayer_task/"))
		{
			candidates.add("Slayer_task/" + cleaned);
		}
	}

	private BufferedImage createIcon(int size)
	{
		try (InputStream stream = SlayerPrepAssistantPlugin.class.getResourceAsStream("/slayer-prep-assistant-icon.png"))
		{
			if (stream != null)
			{
				return ImageUtil.resizeImage(ImageIO.read(stream), size, size);
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
		return configManager.getConfig(SlayerPrepAssistantConfig.class);
	}
}
