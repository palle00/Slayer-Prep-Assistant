package com.slayerprepassistant.wiki;

import com.slayerprepassistant.gear.GearRecommendation;
import com.slayerprepassistant.gear.GearSlot;
import com.slayerprepassistant.gear.GearTier;
import com.slayerprepassistant.gear.RecommendedItem;
import com.slayerprepassistant.guide.CombatMethod;
import com.slayerprepassistant.guide.InventoryRecommendation;
import com.slayerprepassistant.guide.MonsterGuide;
import com.slayerprepassistant.guide.StrategyMethod;
import com.slayerprepassistant.model.ParsingConfidence;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class RenderedStrategyParser
{
	private static final Pattern TABBER_TAB = Pattern.compile("<div class=\"tabbertab\"[^>]*data-title=\"([^\"]+)\"[^>]*>(.*?)(?=<div class=\"tabbertab\"|</div></div>\\s*<!--|$)", Pattern.DOTALL);
	private static final Pattern EQUIPMENT_TABLE = Pattern.compile("<table class=\"equipment equipment-right\"[^>]*>(.*?)</table>", Pattern.DOTALL);
	private static final Pattern INVENTORY_TABLE = Pattern.compile("<table class=\"inventorytable storage-right\"[^>]*>(.*?)</table>", Pattern.DOTALL);
	private static final Pattern EQUIPMENT_DIV = Pattern.compile("<div class=\"equipment-div\"[^>]*>(.*?)(?=<table|<div class=\"tabbertab\"|$)", Pattern.DOTALL);
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
		List<StrategyMethod> htmlMethods = parseMethods(renderedHtml);
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
				continue;
			}
			mergedMethods.add(new StrategyMethod(
				sourceMethod.getMethod(),
				mergeMaxGear(htmlMethod.getGear(), sourceMethod.getGear()),
				htmlMethod.getInventory().isEmpty() ? sourceMethod.getInventory() : htmlMethod.getInventory(),
				sourceMethod.getNotes(),
				sourceMethod.getConfidence()));
		}
		for (StrategyMethod htmlMethod : htmlMethods)
		{
			if (findMethod(mergedMethods, htmlMethod.getMethod()) == null)
			{
				mergedMethods.add(htmlMethod);
			}
		}
		MonsterGuide guide = source.getGuide();
		return new WikiParsingResult(
			new MonsterGuide(guide.getMonsterName(), guide.getWikiTitle(), guide.getWikiUrl(), guide.getRevisionId(), guide.getFetchedAt(), mergedMethods, guide.getNotes(), guide.getConfidence()),
			source.getVariants(),
			source.getWarnings());
	}

	private List<StrategyMethod> parseMethods(String html)
	{
		List<StrategyMethod> methods = new ArrayList<>();
		Matcher tabMatcher = TABBER_TAB.matcher(html);
		while (tabMatcher.find())
		{
			CombatMethod method = methodForHeading(htmlText(tabMatcher.group(1)).toLowerCase(Locale.ROOT));
			if (method == null)
			{
				continue;
			}
			String tabHtml = tabMatcher.group(2);
			List<GearRecommendation> gear = parseEquipment(tabHtml);
			List<InventoryRecommendation> inventory = parseInventory(tabHtml);
			if (!gear.isEmpty() || !inventory.isEmpty())
			{
				methods.add(new StrategyMethod(method, gear, inventory, Collections.emptyList(), ParsingConfidence.HIGH));
			}
		}
		return methods;
	}

	private CombatMethod methodForHeading(String heading)
	{
		if (heading.contains("melee"))
		{
			return CombatMethod.MELEE;
		}
		if (heading.contains("ranged") || heading.contains("range"))
		{
			return CombatMethod.RANGED;
		}
		if (heading.contains("magic") || heading.contains("mage"))
		{
			return CombatMethod.MAGIC;
		}
		return null;
	}

	private StrategyMethod findMethod(List<StrategyMethod> methods, CombatMethod method)
	{
		for (StrategyMethod candidate : methods)
		{
			if (candidate.getMethod() == method)
			{
				return candidate;
			}
		}
		return null;
	}

	private List<GearRecommendation> mergeMaxGear(List<GearRecommendation> maxGear, List<GearRecommendation> alternatives)
	{
		Map<GearSlot, List<GearTier>> bySlot = new EnumMap<>(GearSlot.class);
		for (GearRecommendation recommendation : maxGear)
		{
			bySlot.computeIfAbsent(recommendation.getSlot(), ignored -> new ArrayList<>()).addAll(recommendation.getTiers());
		}
		for (GearRecommendation recommendation : alternatives)
		{
			List<GearTier> tiers = bySlot.computeIfAbsent(recommendation.getSlot(), ignored -> new ArrayList<>());
			int offset = tiers.isEmpty() ? 0 : tiers.stream().mapToInt(GearTier::getPriority).max().orElse(0);
			for (GearTier tier : recommendation.getTiers())
			{
				List<RecommendedItem> items = new ArrayList<>();
				for (RecommendedItem item : tier.getAlternatives())
				{
					if (!containsItem(tiers, item.getName()))
					{
						items.add(item);
					}
				}
				if (!items.isEmpty())
				{
					tiers.add(new GearTier(offset + tier.getPriority(), items));
				}
			}
		}
		List<GearRecommendation> merged = new ArrayList<>();
		for (Map.Entry<GearSlot, List<GearTier>> entry : bySlot.entrySet())
		{
			entry.getValue().sort(java.util.Comparator.comparingInt(GearTier::getPriority));
			merged.add(new GearRecommendation(entry.getKey(), entry.getValue()));
		}
		return merged;
	}

	private boolean containsItem(List<GearTier> tiers, String name)
	{
		for (GearTier tier : tiers)
		{
			for (RecommendedItem item : tier.getAlternatives())
			{
				if (item.getName().equalsIgnoreCase(name))
				{
					return true;
				}
			}
		}
		return false;
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
		List<GearRecommendation> recommendations = new ArrayList<>();
		for (Map.Entry<GearSlot, List<GearTier>> entry : bySlot.entrySet())
		{
			recommendations.add(new GearRecommendation(entry.getKey(), entry.getValue()));
		}
		return recommendations;
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
		if (!tableMatcher.find())
		{
			return Collections.emptyList();
		}
		List<InventoryRecommendation> inventory = new ArrayList<>();
		for (RecommendedItem item : htmlItems(tableMatcher.group(1)))
		{
			inventory.add(new InventoryRecommendation(item.getName(), 0, true));
		}
		return inventory;
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
			if (!hrefMatcher.find() || !titleMatcher.find())
			{
				continue;
			}
			if (hrefMatcher.group(1).contains(":"))
			{
				continue;
			}
			String name = htmlText(titleMatcher.group(1));
			if (isUsableItemTitle(name) && !containsRecommended(items, name))
			{
				items.add(new RecommendedItem(name));
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
		String decoded = URLDecoder.decode(fileName.replace('_', ' '), StandardCharsets.UTF_8);
		return htmlText(decoded);
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
		for (RecommendedItem item : items)
		{
			if (item.getName().equalsIgnoreCase(name))
			{
				return true;
			}
		}
		return false;
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
