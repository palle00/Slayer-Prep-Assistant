package com.slayerprepassistant.wiki;

import com.slayerprepassistant.gear.GearRecommendation;
import com.slayerprepassistant.gear.GearSlot;
import com.slayerprepassistant.gear.GearTier;
import com.slayerprepassistant.gear.RecommendedItem;
import com.slayerprepassistant.guide.CombatMethod;
import com.slayerprepassistant.guide.InventoryRecommendation;
import com.slayerprepassistant.guide.MonsterGuide;
import com.slayerprepassistant.guide.StrategyMethod;
import com.slayerprepassistant.items.ItemResolver;
import com.slayerprepassistant.model.ParsingConfidence;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

class RenderedStrategyParser
{
	private static final int HTML_PATTERN_FLAGS = Pattern.DOTALL | Pattern.CASE_INSENSITIVE;
	private static final Pattern TABBER_TAB = Pattern.compile("<div[^>]*class=\"[^\"]*\\btabbertab\\b[^\"]*\"[^>]*data-title=\"([^\"]+)\"[^>]*>(.*?)(?=<div[^>]*class=\"[^\"]*\\btabbertab\\b|</div></div>\\s*<!--|$)", HTML_PATTERN_FLAGS);
	private static final Pattern TABBER_TITLE = Pattern.compile("<div[^>]*class=\"[^\"]*\\btabbertab\\b[^\"]*\"[^>]*data-title=\"([^\"]+)\"", HTML_PATTERN_FLAGS);
	private static final Pattern EQUIPMENT_TABLE = Pattern.compile("<table[^>]*class=\"(?=[^\"]*\\bequipment\\b)(?=[^\"]*\\bequipment-right\\b)[^\"]*\"[^>]*>(.*?)</table>", HTML_PATTERN_FLAGS);
	private static final Pattern INVENTORY_TABLE = Pattern.compile("<table[^>]*class=\"(?=[^\"]*\\binventorytable\\b)(?=[^\"]*\\bstorage-right\\b)[^\"]*\"[^>]*>(.*?)</table>", HTML_PATTERN_FLAGS);
	private static final Pattern EQUIPMENT_DIV = Pattern.compile("<div[^>]*class=\"[^\"]*\\bequipment-div\\b[^\"]*\"[^>]*>(.*?)(?=<table|<div[^>]*class=\"[^\"]*\\btabbertab\\b|$)", HTML_PATTERN_FLAGS);
	private static final Pattern EQUIPMENT_SLOT_START = Pattern.compile("<div class=\"equipment-(head|cape|neck|ammo2?|weapon|body|torso|shield|legs|hands|gloves|feet|boots|ring)(?:\\s|\"|-)[^\"]*\">", Pattern.DOTALL);
	private static final Pattern HTML_LINK = Pattern.compile("<a\\s+([^>]*)>", Pattern.DOTALL);
	private static final Pattern HTML_HREF = Pattern.compile("href=\"/w/([^\"]+)\"");
	private static final Pattern HTML_TITLE = Pattern.compile("title=\"([^\"]+)\"");
	private static final Pattern HTML_IMAGE = Pattern.compile("(?:<img\\s+([^>]*)>|!\\[[^\\]]*]\\((/images/[^)]+)\\))", Pattern.DOTALL);
	private static final Pattern HTML_SRC = Pattern.compile("(?:src|data-src)=\"([^\"]+)\"");
	private static final Pattern HTML_ALT = Pattern.compile("alt=\"([^\"]+)\"");

	WikiParsingResult mergeInto(WikiParsingResult source, String renderedHtml)
	{
		if (source == null || renderedHtml == null || renderedHtml.isEmpty())
		{
			return source;
		}

		CombatMethod fallbackMethod = fallbackMethod(source);
		List<StrategyMethod> htmlMethods = parseMethods(renderedHtml, fallbackMethod);
		if (htmlMethods.isEmpty())
		{
			return source;
		}

		List<StrategyMethod> mergedMethods = new ArrayList<>();
		for (StrategyMethod sourceMethod : source.getGuide().getMethods())
		{
			StrategyMethod htmlMethod = findMethod(htmlMethods, sourceMethod.getMethod());
			if (htmlMethod == null)
			{
				mergedMethods.add(sourceMethod);
			}
			else
			{
				mergedMethods.add(new StrategyMethod(
						sourceMethod.getMethod(),
						mergeGearPreferRendered(htmlMethod.getGear(), sourceMethod.getGear()),
						mergeInventory(htmlMethod.getInventory(), sourceMethod.getInventory()),
						sourceMethod.getNotes(),
						sourceMethod.getConfidence()));
			}
		}

		htmlMethods.stream()
				.filter(hm -> findMethod(mergedMethods, hm.getMethod()) == null)
				.forEach(mergedMethods::add);

		MonsterGuide guide = source.getGuide();
		return new WikiParsingResult(
				new MonsterGuide(guide.getMonsterName(), guide.getWikiTitle(), guide.getWikiUrl(), guide.getRevisionId(), guide.getFetchedAt(), mergedMethods, guide.getNotes(), guide.getConfidence()),
				source.getVariants(),
				source.getWarnings());
	}

	private CombatMethod fallbackMethod(WikiParsingResult source)
	{
		List<StrategyMethod> methods = source.getGuide().getMethods();
		return methods.size() == 1 ? methods.get(0).getMethod() : null;
	}

	private List<StrategyMethod> parseMethods(String html, CombatMethod fallbackMethod)
	{
		List<StrategyMethod> methods = new ArrayList<>();
		Matcher tabMatcher = TABBER_TAB.matcher(html);
		boolean sawTab = false;
		boolean hasCombatLabeledTab = hasCombatLabeledTab(html);

		while (tabMatcher.find())
		{
			sawTab = true;
			String title = htmlText(tabMatcher.group(1));
			CombatMethod method = methodForHeading(title.toLowerCase(Locale.ROOT));
			String tabHtml = tabMatcher.group(2);

			// A generic outer/location tab (for example "Catacombs") must not inherit
			// the source method when the page also contains explicitly-labelled combat tabs.
			// Doing so is what previously allowed Magic/Ranged gear to be merged into Melee.
			if (method == null && !hasCombatLabeledTab && fallbackMethod != null && equipmentBlockCount(tabHtml) <= 1)
			{
				method = fallbackMethod;
			}
			if (method == null)
			{
				continue;
			}

			List<GearRecommendation> gear = parseEquipment(tabHtml);
			List<InventoryRecommendation> inventory = parseInventory(tabHtml);

			if ((!gear.isEmpty() || !inventory.isEmpty()) && findMethod(methods, method) == null)
			{
				methods.add(new StrategyMethod(method, gear, inventory, Collections.emptyList(), ParsingConfidence.HIGH));
			}
		}
		if (!sawTab && fallbackMethod != null && equipmentBlockCount(html) <= 1)
		{
			List<GearRecommendation> gear = parseEquipment(html);
			List<InventoryRecommendation> inventory = parseInventory(html);
			if (!gear.isEmpty() || !inventory.isEmpty())
			{
				methods.add(new StrategyMethod(fallbackMethod, gear, inventory, Collections.emptyList(), ParsingConfidence.HIGH));
			}
		}
		return methods;
	}

	private boolean hasCombatLabeledTab(String html)
	{
		Matcher matcher = TABBER_TITLE.matcher(html);
		while (matcher.find())
		{
			if (methodForHeading(htmlText(matcher.group(1)).toLowerCase(Locale.ROOT)) != null)
			{
				return true;
			}
		}
		return false;
	}

	private int equipmentBlockCount(String html)
	{
		if (html == null || html.isEmpty())
		{
			return 0;
		}
		int count = 0;
		Matcher tableMatcher = EQUIPMENT_TABLE.matcher(html);
		while (tableMatcher.find())
		{
			count++;
		}
		if (count > 0)
		{
			return count;
		}
		Matcher divMatcher = EQUIPMENT_DIV.matcher(html);
		while (divMatcher.find())
		{
			count++;
		}
		return count;
	}

	private CombatMethod methodForHeading(String heading)
	{
		if (heading.contains("melee")) return CombatMethod.MELEE;
		if (heading.contains("ranged") || heading.contains("range")) return CombatMethod.RANGED;
		if (heading.contains("magic") || heading.contains("mage")) return CombatMethod.MAGIC;
		return null;
	}

	private StrategyMethod findMethod(List<StrategyMethod> methods, CombatMethod method)
	{
		return methods.stream()
				.filter(m -> m.getMethod() == method)
				.findFirst()
				.orElse(null);
	}

	private List<GearRecommendation> mergeGearPreferRendered(List<GearRecommendation> rendered, List<GearRecommendation> source)
	{
		Map<GearSlot, GearRecommendation> bySlot = new EnumMap<>(GearSlot.class);
		if (source != null)
		{
			for (GearRecommendation recommendation : source)
			{
				if (recommendation != null && recommendation.getSlot() != null)
				{
					bySlot.put(recommendation.getSlot(), recommendation);
				}
			}
		}
		if (rendered != null)
		{
			for (GearRecommendation recommendation : rendered)
			{
				if (recommendation != null && recommendation.getSlot() != null && recommendation.getTiers() != null && !recommendation.getTiers().isEmpty())
				{
					GearRecommendation sourceRecommendation = bySlot.get(recommendation.getSlot());
					if (sourceRecommendation == null
							|| !hasRankedAlternatives(sourceRecommendation)
							|| isRicherRecommendation(recommendation, sourceRecommendation))
					{
						bySlot.put(recommendation.getSlot(), recommendation);
					}
				}
			}
		}
		return new ArrayList<>(bySlot.values());
	}

	private boolean hasRankedAlternatives(GearRecommendation recommendation)
	{
		return tierCount(recommendation) > 1 || itemCount(recommendation) > 1;
	}

	private boolean isRicherRecommendation(GearRecommendation candidate, GearRecommendation existing)
	{
		return itemCount(candidate) > itemCount(existing) || tierCount(candidate) > tierCount(existing);
	}

	private int tierCount(GearRecommendation recommendation)
	{
		return recommendation == null || recommendation.getTiers() == null ? 0 : recommendation.getTiers().size();
	}

	private int itemCount(GearRecommendation recommendation)
	{
		if (recommendation == null || recommendation.getTiers() == null)
		{
			return 0;
		}
		int count = 0;
		for (GearTier tier : recommendation.getTiers())
		{
			if (tier != null && tier.getAlternatives() != null)
			{
				count += tier.getAlternatives().size();
			}
		}
		return count;
	}

	private List<InventoryRecommendation> mergeInventory(List<InventoryRecommendation> rendered, List<InventoryRecommendation> source)
	{
		if ((rendered == null || rendered.isEmpty()) && (source == null || source.isEmpty()))
		{
			return Collections.emptyList();
		}

		Map<String, InventoryRecommendation> merged = new LinkedHashMap<>();
		if (rendered != null)
		{
			for (InventoryRecommendation recommendation : rendered)
			{
				mergeInventoryRecommendation(merged, recommendation);
			}
		}
		if (source != null)
		{
			for (InventoryRecommendation recommendation : source)
			{
				mergeInventoryRecommendation(merged, recommendation);
			}
		}
		return new ArrayList<>(merged.values());
	}

	private void mergeInventoryRecommendation(Map<String, InventoryRecommendation> merged, InventoryRecommendation recommendation)
	{
		if (recommendation == null)
		{
			return;
		}
		String key = ItemResolver.canonicalKey(recommendation.getItemOrCategory());
		if (key.isEmpty())
		{
			return;
		}
		InventoryRecommendation existing = merged.get(key);
		if (existing == null)
		{
			merged.put(key, recommendation);
			return;
		}
		merged.put(key, new InventoryRecommendation(
			existing.getItemOrCategory(),
			Math.max(existing.getMinimumQuantity(), recommendation.getMinimumQuantity()),
			existing.isRequired() || recommendation.isRequired()));
	}

	private List<GearRecommendation> parseEquipment(String html)
	{
		List<String> blocks = equipmentBlocks(html);
		if (blocks.isEmpty())
		{
			return Collections.emptyList();
		}

		Map<GearSlot, List<GearTier>> bySlot = new EnumMap<>(GearSlot.class);
		for (String tableHtml : blocks)
		{
			Matcher slotMatcher = EQUIPMENT_SLOT_START.matcher(tableHtml);
			while (slotMatcher.find())
			{
				GearSlot slot = WikiGearSlots.parse(slotMatcher.group(1));
				int slotStart = slotMatcher.end();
				int slotEnd = nextSlotStart(tableHtml, slotStart);
				List<RecommendedItem> items = htmlItems(tableHtml.substring(slotStart, slotEnd));

				if (slot != null && !items.isEmpty())
				{
					bySlot.put(slot, Collections.singletonList(new GearTier(0, items)));
				}
			}
		}

		return bySlot.entrySet().stream()
				.map(entry -> new GearRecommendation(entry.getKey(), entry.getValue()))
				.collect(Collectors.toList());
	}

	private List<String> equipmentBlocks(String html)
	{
		List<String> blocks = new ArrayList<>();
		Matcher tableMatcher = EQUIPMENT_TABLE.matcher(html);
		while (tableMatcher.find())
		{
			blocks.add(tableMatcher.group(1));
		}
		if (!blocks.isEmpty())
		{
			return blocks;
		}
		Matcher divMatcher = EQUIPMENT_DIV.matcher(html);
		while (divMatcher.find())
		{
			blocks.add(divMatcher.group(1));
		}
		return blocks;
	}

	private int nextSlotStart(String html, int start)
	{
		Matcher matcher = EQUIPMENT_SLOT_START.matcher(html);
		return matcher.find(start) ? matcher.start() : html.length();
	}

	private List<InventoryRecommendation> parseInventory(String html)
	{
		Matcher tableMatcher = INVENTORY_TABLE.matcher(html);
		Map<String, InventoryRecommendation> inventory = new LinkedHashMap<>();
		while (tableMatcher.find())
		{
			for (RecommendedItem item : htmlItems(tableMatcher.group(1)))
			{
				mergeInventoryRecommendation(inventory, new InventoryRecommendation(item.getName(), 0, true));
			}
		}
		return inventory.isEmpty() ? Collections.emptyList() : new ArrayList<>(inventory.values());
	}

	private List<RecommendedItem> htmlItems(String html)
	{
		List<RecommendedItem> items = new ArrayList<>();

		Matcher linkMatcher = HTML_LINK.matcher(html);
		while (linkMatcher.find())
		{
			String attributes = linkMatcher.group(1);
			Matcher hrefMatcher = HTML_HREF.matcher(attributes);
			Matcher titleMatcher = HTML_TITLE.matcher(attributes);

			if (hrefMatcher.find() && titleMatcher.find() && !hrefMatcher.group(1).contains(":"))
			{
				String name = htmlText(titleMatcher.group(1));
				if (isUsableItemTitle(name) && !containsRecommended(items, name))
				{
					items.add(new RecommendedItem(name));
				}
			}
		}

		Matcher imageMatcher = HTML_IMAGE.matcher(html);
		while (imageMatcher.find())
		{
			String attributes = imageMatcher.group(1);
			String markdownSrc = imageMatcher.group(2);
			String name = itemNameFromImage(attributes, markdownSrc);
			if (isUsableItemTitle(name) && !containsRecommended(items, name))
			{
				items.add(new RecommendedItem(name));
			}
		}
		return items;
	}

	private String itemNameFromImage(String attributes, String markdownSrc)
	{
		if (attributes != null)
		{
			Matcher altMatcher = HTML_ALT.matcher(attributes);
			if (altMatcher.find())
			{
				String alt = htmlText(altMatcher.group(1));
				if (isUsableItemTitle(alt))
				{
					return alt.replaceFirst("\\.png$", "");
				}
			}
			Matcher srcMatcher = HTML_SRC.matcher(attributes);
			if (srcMatcher.find())
			{
				return itemNameFromImagePath(srcMatcher.group(1));
			}
		}
		return itemNameFromImagePath(markdownSrc);
	}

	private String itemNameFromImagePath(String src)
	{
		if (src == null || src.trim().isEmpty())
		{
			return "";
		}
		String path = src;
		int query = path.indexOf('?');
		if (query >= 0)
		{
			path = path.substring(0, query);
		}
		int slash = path.lastIndexOf('/');
		String fileName = slash >= 0 ? path.substring(slash + 1) : path;
		if (fileName.toLowerCase(Locale.ROOT).endsWith(".png"))
		{
			fileName = fileName.substring(0, fileName.length() - ".png".length());
		}
		if (fileName.matches(".*%29_\\d+$"))
		{
			fileName = fileName.replaceFirst("_\\d+$", "");
		}
		try
		{
			String decoded = URLDecoder.decode(fileName.replace('_', ' '), StandardCharsets.UTF_8);
			return htmlText(decoded);
		}
		catch (IllegalArgumentException ex)
		{
			return htmlText(fileName.replace('_', ' '));
		}
	}

	private boolean isUsableItemTitle(String name)
	{
		String normalized = name.toLowerCase(Locale.ROOT).trim();
		return !normalized.isEmpty()
				&& !normalized.endsWith(" slot")
				&& !normalized.contains("placeholder")
				&& !normalized.equals("special attack")
				&& !normalized.equals("empty")
				&& !normalized.equals("n/a")
				&& !normalized.equals("none");
	}

	private boolean containsRecommended(List<RecommendedItem> items, String name)
	{
		return items.stream().anyMatch(item -> item.getName().equalsIgnoreCase(name));
	}

	private String htmlText(String value)
	{
		return value == null ? "" : value
				.replace("&amp;", "&")
				.replace("&#39;", "'")
				.replace("&#039;", "'")
				.replace("&apos;", "'")
				.replace("&quot;", "\"")
				.replace("&nbsp;", " ")
				.replaceAll("\\s+", " ")
				.trim();
	}
}
