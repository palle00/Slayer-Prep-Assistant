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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
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
	private final RenderedStrategyParser renderedStrategyParser = new RenderedStrategyParser();

	public WikiParsingResult parse(String monsterName, String wikiTitle, String wikiUrl, long revisionId, String text)
	{
		List<String> warnings = new ArrayList<>();
		Map<CombatMethod, List<String>> methodLines = new EnumMap<>(CombatMethod.class);
		for (CombatMethod method : CombatMethod.values())
		{
			methodLines.put(method, new ArrayList<>());
		}
		List<InventoryRecommendation> sharedInventory = new ArrayList<>();
		Map<CombatMethod, List<InventoryRecommendation>> methodInventory = new EnumMap<>(CombatMethod.class);
		for (CombatMethod method : CombatMethod.values())
		{
			methodInventory.put(method, new ArrayList<>());
		}
		List<String> notes = new ArrayList<>();
		List<MonsterVariant> variants = new ArrayList<>();
		List<CombatMethod> methodOrder = new ArrayList<>();
		List<String> inventoryTemplateLines = new ArrayList<>();
		CombatMethod inventoryTemplateMethod = CombatMethod.defaultMethod();
		boolean collectingInventoryTemplate = false;

		String section = "general";
		CombatMethod currentMethod = CombatMethod.defaultMethod();
		for (String rawLine : text.split("\\R"))
		{
			String line = rawLine.trim();
			if (line.isEmpty())
			{
				continue;
			}
			if (collectingInventoryTemplate)
			{
				inventoryTemplateLines.add(line);
				if (isTemplateEnd(line))
				{
					methodInventory.get(inventoryTemplateMethod).addAll(parseInventoryTemplate(inventoryTemplateLines));
					inventoryTemplateLines.clear();
					collectingInventoryTemplate = false;
				}
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
			if (isInventoryTemplateStart(line))
			{
				inventoryTemplateMethod = currentMethod;
				inventoryTemplateLines.add(line);
				if (isTemplateEnd(line))
				{
					methodInventory.get(inventoryTemplateMethod).addAll(parseInventoryTemplate(inventoryTemplateLines));
					inventoryTemplateLines.clear();
				}
				else
				{
					collectingInventoryTemplate = true;
				}
				continue;
			}
			if ("variants".equals(section))
			{
				parseVariants(line, variants);
			}
			else if ("inventory".equals(section))
			{
				sharedInventory.add(new InventoryRecommendation(stripBullet(line), 0, true));
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
				methods.add(new StrategyMethod(method, gear, inventoryForMethod(method, methodInventory, sharedInventory), notes, ParsingConfidence.MEDIUM));
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
				methods.add(new StrategyMethod(entry.getKey(), gear, inventoryForMethod(entry.getKey(), methodInventory, sharedInventory), notes, ParsingConfidence.MEDIUM));
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
		return renderedStrategyParser.mergeInto(source, renderedHtml);
	}

	private List<InventoryRecommendation> inventoryForMethod(CombatMethod method, Map<CombatMethod, List<InventoryRecommendation>> methodInventory, List<InventoryRecommendation> sharedInventory)
	{
		List<InventoryRecommendation> inventory = methodInventory.get(method);
		return inventory == null || inventory.isEmpty() ? sharedInventory : inventory;
	}

	private boolean isInventoryTemplateStart(String line)
	{
		return line.toLowerCase(Locale.ROOT).startsWith("{{inventory");
	}

	private boolean isTemplateEnd(String line)
	{
		return line.trim().equals("}}") || line.trim().endsWith("}}");
	}

	private List<InventoryRecommendation> parseInventoryTemplate(List<String> lines)
	{
		String block = String.join("\n", lines);
		int start = block.toLowerCase(Locale.ROOT).indexOf("{{inventory");
		if (start < 0)
		{
			return Collections.emptyList();
		}
		int end = block.lastIndexOf("}}");
		String content = end > start
			? block.substring(start + "{{Inventory".length(), end)
			: block.substring(start + "{{Inventory".length());
		Map<String, Integer> counts = new LinkedHashMap<>();
		for (String parameter : splitTemplateParameters(content))
		{
			String itemName = inventoryTemplateItem(parameter);
			if (!itemName.isEmpty())
			{
				counts.merge(itemName, 1, Integer::sum);
			}
		}
		List<InventoryRecommendation> inventory = new ArrayList<>();
		for (Map.Entry<String, Integer> entry : counts.entrySet())
		{
			inventory.add(new InventoryRecommendation(entry.getKey(), entry.getValue(), true));
		}
		return inventory;
	}

	private List<String> splitTemplateParameters(String content)
	{
		List<String> parameters = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		int templateDepth = 0;
		int linkDepth = 0;
		for (int i = 0; i < content.length(); i++)
		{
			char character = content.charAt(i);
			char next = i + 1 < content.length() ? content.charAt(i + 1) : 0;
			if (character == '{' && next == '{')
			{
				templateDepth++;
				current.append(character).append(next);
				i++;
				continue;
			}
			if (character == '}' && next == '}')
			{
				templateDepth = Math.max(0, templateDepth - 1);
				current.append(character).append(next);
				i++;
				continue;
			}
			if (character == '[' && next == '[')
			{
				linkDepth++;
				current.append(character).append(next);
				i++;
				continue;
			}
			if (character == ']' && next == ']')
			{
				linkDepth = Math.max(0, linkDepth - 1);
				current.append(character).append(next);
				i++;
				continue;
			}
			if (character == '|' && templateDepth == 0 && linkDepth == 0)
			{
				parameters.add(current.toString());
				current.setLength(0);
				continue;
			}
			current.append(character);
		}
		parameters.add(current.toString());
		return parameters;
	}

	private String inventoryTemplateItem(String parameter)
	{
		String value = parameter == null ? "" : parameter.trim();
		if (value.isEmpty())
		{
			return "";
		}
		int equals = value.indexOf('=');
		if (equals >= 0)
		{
			String key = value.substring(0, equals).trim().toLowerCase(Locale.ROOT);
			String assignedValue = value.substring(equals + 1).trim();
			if (key.matches("\\d+"))
			{
				value = assignedValue;
			}
			else if ("align".equals(key) || "caption".equals(key) || "name".equals(key) || "style".equals(key))
			{
				return "";
			}
		}
		return cleanItemName(value);
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
		String normalized = line.trim().toLowerCase(Locale.ROOT)
			.replaceFirst("^\\|-\\|\\s*", "")
			.trim();
		if (normalized.matches("^(ranged|range)(?:\\s*\\([^)]*\\))?\\s*=\\s*$"))
		{
			return CombatMethod.RANGED;
		}
		if (normalized.matches("^melee(?:\\s*\\([^)]*\\))?\\s*=\\s*$"))
		{
			return CombatMethod.MELEE;
		}
		if (normalized.matches("^(magic|mage)(?:\\s*\\([^)]*\\))?\\s*=\\s*$"))
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
			GearSlot slot = WikiGearSlots.parse(slotSplit[0]);
			if (slot == null)
			{
				continue;
			}
			Integer templateRank = WikiGearSlots.templateRank(slotSplit[0]);
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
