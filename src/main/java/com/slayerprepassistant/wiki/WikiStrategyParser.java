package com.slayerprepassistant.wiki;

import com.slayerprepassistant.gear.GearRecommendation;
import com.slayerprepassistant.gear.GearSlot;
import com.slayerprepassistant.gear.GearTier;
import com.slayerprepassistant.gear.RecommendedItem;
import com.slayerprepassistant.guide.CombatMethod;
import com.slayerprepassistant.guide.InventoryRecommendation;
import com.slayerprepassistant.guide.MonsterGuide;
import com.slayerprepassistant.guide.QuantityMode;
import com.slayerprepassistant.guide.StrategyMethod;
import com.slayerprepassistant.model.ParsingConfidence;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WikiStrategyParser
{
	private static final Pattern WIKI_HEADING = Pattern.compile("^=+\\s*(.+?)\\s*=+$");
	private static final Pattern WIKI_LINK = Pattern.compile("\\[\\[([^\\]|#]+)(?:#[^\\]|]+)?(?:\\|([^\\]]+))?\\]\\]");
	private static final Pattern TEMPLATE_LINK = Pattern.compile("\\{\\{\\s*[Pp]link\\s*\\|\\s*([^\\}|]+)(?:\\|[^\\}]*)?\\}\\}");
	private static final Pattern SLOT_RANK = Pattern.compile("^\\|?\\s*[A-Za-z ]+(\\d+)\\s*$");
	private static final Pattern TABBER_TAB = Pattern.compile("<div class=\"tabbertab\"[^>]*data-title=\"([^\"]+)\"[^>]*>(.*?)(?=<div class=\"tabbertab\"|</div></div>\\s*<!--|$)", Pattern.DOTALL);
	private static final Pattern EQUIPMENT_TABLE = Pattern.compile("<table class=\"equipment equipment-right\"[^>]*>(.*?)</table>", Pattern.DOTALL);
	private static final Pattern INVENTORY_TABLE = Pattern.compile("<table class=\"inventorytable storage-right\"[^>]*>(.*?)</table>", Pattern.DOTALL);
	private static final Pattern EQUIPMENT_SLOT_START = Pattern.compile("<div class=\"equipment-(head|cape|neck|ammo|weapon|body|shield|legs|hands|feet|ring)[^\"]*\">", Pattern.DOTALL);
	private static final Pattern HTML_LINK = Pattern.compile("<a\\s+([^>]*)>", Pattern.DOTALL);
	private static final Pattern HTML_HREF = Pattern.compile("href=\"/w/([^\"]+)\"");
	private static final Pattern HTML_TITLE = Pattern.compile("title=\"([^\"]+)\"");

	public WikiParsingResult parse(String monsterName, String wikiTitle, String wikiUrl, long revisionId, String text)
	{
		List<String> warnings = new ArrayList<>();
		Map<CombatMethod, List<String>> methodLines = new EnumMap<>(CombatMethod.class);
		for (CombatMethod method : CombatMethod.values())
		{
			methodLines.put(method, new ArrayList<>());
		}
		List<InventoryRecommendation> inventory = new ArrayList<>();
		List<String> notes = new ArrayList<>();
		List<MonsterVariant> variants = new ArrayList<>();
		List<CombatMethod> methodOrder = new ArrayList<>();

		String section = "general";
		CombatMethod currentMethod = CombatMethod.GENERAL;
		for (String rawLine : text.split("\\R"))
		{
			String line = rawLine.trim();
			if (line.isEmpty())
			{
				continue;
			}
			String heading = heading(line);
			if (heading != null)
			{
				String normalizedHeading = heading.toLowerCase(Locale.ROOT);
				section = categorize(normalizedHeading);
				CombatMethod headingMethod = methodForHeading(normalizedHeading);
				if (headingMethod != null)
				{
					currentMethod = headingMethod;
					rememberMethod(methodOrder, currentMethod);
				}
				continue;
			}
			CombatMethod tabberMethod = methodForTabberLine(line);
			if (tabberMethod != null)
			{
				currentMethod = tabberMethod;
				rememberMethod(methodOrder, currentMethod);
				section = "equipment";
				continue;
			}
			if ("variants".equals(section))
			{
				parseVariants(line, variants);
			}
			else if ("inventory".equals(section))
			{
				inventory.add(new InventoryRecommendation(stripBullet(line), QuantityMode.UNSPECIFIED, 0, 0, true, "", Collections.emptyList()));
			}
			else if ("equipment".equals(section) || "method".equals(section))
			{
				methodLines.get(currentMethod).add(stripBullet(line));
			}
			else
			{
				notes.add(stripBullet(line));
			}
		}

		List<StrategyMethod> methods = new ArrayList<>();
		for (CombatMethod method : methodOrder)
		{
			List<GearRecommendation> gear = parseGear(methodLines.get(method));
			if (!gear.isEmpty())
			{
				methods.add(new StrategyMethod(method, gear, inventory, notes, ParsingConfidence.MEDIUM));
			}
		}
		for (Map.Entry<CombatMethod, List<String>> entry : methodLines.entrySet())
		{
			if (methodOrder.contains(entry.getKey()))
			{
				continue;
			}
			List<GearRecommendation> gear = parseGear(entry.getValue());
			if (!gear.isEmpty())
			{
				methods.add(new StrategyMethod(entry.getKey(), gear, inventory, notes, ParsingConfidence.MEDIUM));
			}
		}
		if (methods.isEmpty())
		{
			warnings.add("No combat method sections were parsed.");
		}

		MonsterGuide guide = new MonsterGuide(monsterName, wikiTitle, wikiUrl, revisionId, Instant.now(), methods, notes, warnings.isEmpty() ? ParsingConfidence.MEDIUM : ParsingConfidence.LOW);
		return new WikiParsingResult(guide, variants, warnings);
	}

	public WikiParsingResult mergeRenderedHtml(WikiParsingResult source, String renderedHtml)
	{
		if (source == null || renderedHtml == null || renderedHtml.isEmpty())
		{
			return source;
		}
		List<StrategyMethod> htmlMethods = parseRenderedMethods(renderedHtml);
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

	private List<StrategyMethod> parseRenderedMethods(String html)
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
			List<GearRecommendation> gear = parseRenderedEquipment(tabHtml);
			List<InventoryRecommendation> inventory = parseRenderedInventory(tabHtml);
			if (!gear.isEmpty() || !inventory.isEmpty())
			{
				methods.add(new StrategyMethod(method, gear, inventory, Collections.emptyList(), ParsingConfidence.HIGH));
			}
		}
		return methods;
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

	private List<GearRecommendation> parseRenderedEquipment(String html)
	{
		Matcher tableMatcher = EQUIPMENT_TABLE.matcher(html);
		if (!tableMatcher.find())
		{
			return Collections.emptyList();
		}
		Map<GearSlot, List<GearTier>> bySlot = new EnumMap<>(GearSlot.class);
		String tableHtml = tableMatcher.group(1);
		Matcher slotMatcher = EQUIPMENT_SLOT_START.matcher(tableHtml);
		while (slotMatcher.find())
		{
			GearSlot slot = parseSlot(slotMatcher.group(1));
			int slotStart = slotMatcher.end();
			int slotEnd = nextSlotStart(tableHtml, slotStart);
			List<RecommendedItem> items = htmlTitles(tableHtml.substring(slotStart, slotEnd));
			if (slot != null && !items.isEmpty())
			{
				bySlot.put(slot, Collections.singletonList(new GearTier(0, items)));
			}
		}
		List<GearRecommendation> recommendations = new ArrayList<>();
		for (Map.Entry<GearSlot, List<GearTier>> entry : bySlot.entrySet())
		{
			recommendations.add(new GearRecommendation(entry.getKey(), entry.getValue()));
		}
		return recommendations;
	}

	private int nextSlotStart(String html, int start)
	{
		Matcher matcher = EQUIPMENT_SLOT_START.matcher(html);
		return matcher.find(start) ? matcher.start() : html.length();
	}

	private List<InventoryRecommendation> parseRenderedInventory(String html)
	{
		Matcher tableMatcher = INVENTORY_TABLE.matcher(html);
		if (!tableMatcher.find())
		{
			return Collections.emptyList();
		}
		List<InventoryRecommendation> inventory = new ArrayList<>();
		for (RecommendedItem item : htmlTitles(tableMatcher.group(1)))
		{
			inventory.add(new InventoryRecommendation(item.getName(), QuantityMode.UNSPECIFIED, 0, 0, true, "", Collections.emptyList()));
		}
		return inventory;
	}

	private List<RecommendedItem> htmlTitles(String html)
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
		return items;
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

	private String categorize(String heading)
	{
		if (heading.contains("monster variant"))
		{
			return "variants";
		}
		if (heading.contains("inventory") || heading.contains("supplies"))
		{
			return "inventory";
		}
		if (heading.contains("equipment") || heading.contains("gear") || heading.contains("melee") || heading.contains("ranged") || heading.contains("magic"))
		{
			return "equipment";
		}
		return "general";
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

	private void rememberMethod(List<CombatMethod> methodOrder, CombatMethod method)
	{
		if (!methodOrder.contains(method))
		{
			methodOrder.add(method);
		}
	}

	private CombatMethod methodForTabberLine(String line)
	{
		String normalized = line.trim().toLowerCase(Locale.ROOT);
		if (normalized.matches("^(ranged|range)\\s*=\\s*$"))
		{
			return CombatMethod.RANGED;
		}
		if (normalized.matches("^melee\\s*=\\s*$"))
		{
			return CombatMethod.MELEE;
		}
		if (normalized.matches("^(magic|mage)\\s*=\\s*$"))
		{
			return CombatMethod.MAGIC;
		}
		return null;
	}

	private void parseVariants(String line, List<MonsterVariant> variants)
	{
		Matcher matcher = WIKI_LINK.matcher(line);
		while (matcher.find())
		{
			String pageTitle = matcher.group(1).trim();
			if (pageTitle.isEmpty() || pageTitle.contains(":"))
			{
				continue;
			}
			String displayName = matcher.group(2) == null ? pageTitle : matcher.group(2).trim();
			if (displayName.isEmpty() || displayName.matches("\\d+"))
			{
				continue;
			}
			String wikiUrl = WikiTitles.pageUrl(pageTitle);
			boolean alreadyPresent = variants.stream().anyMatch(variant -> variant.getWikiPageTitle().equalsIgnoreCase(pageTitle));
			if (!alreadyPresent)
			{
				variants.add(new MonsterVariant(displayName, pageTitle, wikiUrl));
			}
		}
	}

	private String heading(String line)
	{
		if (line.startsWith("#"))
		{
			return line.replace("#", "").trim();
		}
		Matcher matcher = WIKI_HEADING.matcher(line);
		return matcher.matches() ? matcher.group(1).trim() : null;
	}

	private List<GearRecommendation> parseGear(List<String> lines)
	{
		Map<GearSlot, List<GearTier>> tiersBySlot = new EnumMap<>(GearSlot.class);
		for (String line : lines)
		{
			String[] slotSplit = line.split("[:=]", 2);
			if (slotSplit.length != 2)
			{
				continue;
			}
			GearSlot slot = parseSlot(slotSplit[0]);
			if (slot == null)
			{
				continue;
			}
			Integer templateRank = parseTemplateRank(slotSplit[0]);
			String[] tierParts = slotSplit[1].split(">");
			for (int i = 0; i < tierParts.length; i++)
			{
				String[] alternatives = tierParts[i].replace("<br/>", "/").replace("<br>", "/").split("/");
				List<RecommendedItem> items = new ArrayList<>();
				for (String alternative : alternatives)
				{
					String name = cleanItemName(alternative);
					if (!name.isEmpty())
					{
						items.add(new RecommendedItem(name));
					}
				}
				if (!items.isEmpty())
				{
					int priority = templateRank == null ? i + 1 : templateRank + i;
					tiersBySlot.computeIfAbsent(slot, ignored -> new ArrayList<>()).add(new GearTier(priority, items));
				}
			}
		}
		List<GearRecommendation> recommendations = new ArrayList<>();
		for (Map.Entry<GearSlot, List<GearTier>> entry : tiersBySlot.entrySet())
		{
			entry.getValue().sort(java.util.Comparator.comparingInt(GearTier::getPriority));
			recommendations.add(new GearRecommendation(entry.getKey(), entry.getValue()));
		}
		return recommendations;
	}

	private Integer parseTemplateRank(String slotKey)
	{
		Matcher matcher = SLOT_RANK.matcher(slotKey == null ? "" : slotKey.trim());
		return matcher.matches() ? Integer.parseInt(matcher.group(1)) : null;
	}

	private GearSlot parseSlot(String value)
	{
		String normalized = value.trim()
			.replaceFirst("^\\|", "")
			.replaceFirst("\\d+$", "")
			.trim()
			.toUpperCase(Locale.ROOT)
			.replace(' ', '_');
		if ("TORSO".equals(normalized))
		{
			normalized = "BODY";
		}
		if ("AMMUNITION".equals(normalized))
		{
			normalized = "AMMO";
		}
		if ("GLOVES".equals(normalized))
		{
			normalized = "HANDS";
		}
		if ("BOOTS".equals(normalized))
		{
			normalized = "FEET";
		}
		try
		{
			return GearSlot.valueOf(normalized);
		}
		catch (IllegalArgumentException ex)
		{
			return null;
		}
	}

	private String stripBullet(String line)
	{
		return line.replaceFirst("^[*\\-]+\\s*", "").trim();
	}

	private String cleanItemName(String value)
	{
		String cleaned = stripBullet(value)
			.replaceAll("(?i)<br\\s*/?>", " ")
			.replaceAll("\\{\\{efn[^}]*}}", "")
			.replaceAll("\\[\\[[^\\]|]+\\|([^\\]]+)]]", "$1")
			.replaceAll("\\[\\[([^\\]]+)]]", "$1")
			.replaceAll("''+", "")
			.trim();
		Matcher templateMatcher = TEMPLATE_LINK.matcher(cleaned);
		if (templateMatcher.find())
		{
			cleaned = templateMatcher.group(1).trim();
		}
		cleaned = cleaned.replaceAll("\\{\\{[^|}]+\\|([^}|]+)(?:\\|[^}]*)?}}", "$1")
			.replaceAll("\\{\\{[^}]+}}", "")
			.replaceAll("\\([^)]*if on a[^)]*\\)", "")
			.replaceAll("\\s+", " ")
			.trim();
		return cleaned.matches("(?i)n/?a|none|-") ? "" : cleaned;
	}
}
