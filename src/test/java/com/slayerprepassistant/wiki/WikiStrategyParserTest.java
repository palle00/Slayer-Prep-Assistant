package com.slayerprepassistant.wiki;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.slayerprepassistant.gear.GearRecommendation;
import com.slayerprepassistant.gear.GearSlot;
import com.slayerprepassistant.guide.CombatMethod;
import org.junit.Test;

public class WikiStrategyParserTest
{
	@Test
	public void parsesMultipleCombatMethodsAndEquipmentSlots()
	{
		String text = "# Ranged equipment\nWeapon: Toxic blowpipe > Rune crossbow / Magic shortbow\nAmmo: Amethyst darts > Broad bolts\n# Magic equipment\nWeapon: Kodai wand > Ancient staff\n# Inventory\n* Prayer potion\n* Food";

		WikiParsingResult result = new WikiStrategyParser().parse("Fixture", "Strategies/Fixture", "https://oldschool.runescape.wiki/w/Strategies/Fixture", 123, text);

		assertFalse(result.getGuide().getMethods().isEmpty());
		assertEquals(CombatMethod.RANGED, result.getGuide().getMethods().get(0).getMethod());
		assertEquals(2, result.getGuide().getMethods().get(0).getInventory().size());
	}

	@Test
	public void parsesMonsterVariantsFromWikiSection()
	{
		String text = "==Monster variants==\n{| class=\"wikitable\"\n|-\n| [[Abyssal demon]]\n|-\n| [[Greater abyssal demon|Greater Abyssal Demon]]\n|-\n| [[File:Ignored.png]]\n|}";

		WikiParsingResult result = new WikiStrategyParser().parse("Abyssal demons", "Abyssal demon", "https://oldschool.runescape.wiki/w/Abyssal_demon", 456, text);

		assertEquals(2, result.getVariants().size());
		assertEquals("Abyssal demon", result.getVariants().get(0).getDisplayName());
		assertEquals("Greater Abyssal Demon", result.getVariants().get(1).getDisplayName());
		assertEquals("Greater abyssal demon", result.getVariants().get(1).getWikiPageTitle());
	}

	@Test
	public void parsesTabbedEquipmentTemplates()
	{
		String text = "==Equipment==\n<tabber>\nRanged =\n{{Equipment|align = right\n|head = Slayer helmet (i)\n|weapon = Dragon dart\n|shield = V's shield\n}}\n===Equipment upgrades and downgrades===\n{{Recommended equipment\n|style = Ranged\n|head1 = {{plink|Slayer helmet (i)}}\n|weapon1 = {{plink|Dragon knife}} /<br/> {{plink|Dragon dart}}\n|shield1 = {{plink|V's shield}}\n}}\n|-|\nMelee=\n{{Equipment|align = right\n|head = Slayer helmet (i)\n|weapon = Inquisitor's mace\n|shield = V's shield\n}}\n|-|\nMagic=\n{{Equipment|align = right\n|head = Slayer helmet (i)\n|weapon = Eye of Ayak\n|shield = V's shield\n}}\n</tabber>";

		WikiParsingResult result = new WikiStrategyParser().parse("Basilisk Knight", "Basilisk Knight/Strategies", "https://oldschool.runescape.wiki/w/Basilisk_Knight/Strategies", 789, text);

		assertEquals(3, result.getGuide().getMethods().size());
		assertEquals(CombatMethod.RANGED, result.getGuide().getMethods().get(0).getMethod());
		assertEquals(CombatMethod.MELEE, result.getGuide().getMethods().get(1).getMethod());
		assertEquals(CombatMethod.MAGIC, result.getGuide().getMethods().get(2).getMethod());
		assertFalse(result.getGuide().getMethods().get(0).getGear().isEmpty());
	}

	@Test
	public void parsesDustDevilTabbedInventoryAndSameLineTabLabels()
	{
		String text = "==Equipment==\n"
			+ "<tabber>\n"
			+ "|-|\n"
			+ "Magic (Barrage)=\n"
			+ "{{Inventory\n"
			+ "|align = right|Toxic blowpipe|Prayer potion|Prayer potion|Silklined herb sack|Gem bag|Dinh's bulwark|||||||||||||||||Bracelet of slaughter|Bracelet of slaughter|Explorer's ring 4|Bonecrusher|Teleport to house (tablet)|Rune pouch\n"
			+ "}}\n"
			+ "{{Recommended equipment\n"
			+ "|style = Magic (Barrage)\n"
			+ "|head1 = {{plink|Slayer helmet (i)}}\n"
			+ "|body1 = {{Plink|Virtus robe top}}\n"
			+ "|hands1 = {{plink|Bracelet of slaughter}} / {{plink|Expeditious bracelet}}\n"
			+ "|feet1 = {{plink|Avernic treads (max)}}\n"
			+ "}}\n"
			+ "|-|Melee (Hallowfell)=\n"
			+ "{{Recommended equipment\n"
			+ "|style = Melee\n"
			+ "|head1 = {{plink|Slayer helmet (i)}}\n"
			+ "|body1 = {{plink|Torva platebody}} > {{plink|Oathplate chest}}\n"
			+ "|hands1 = {{plink|Expeditious bracelet}} / {{plink|Bracelet of slaughter}}\n"
			+ "|feet1 = {{plink|Avernic treads (max)}}\n"
			+ "|weapon1 = {{plink|Hallowfell}}\n"
			+ "}}\n"
			+ "</tabber>";

		WikiParsingResult result = new WikiStrategyParser().parse("Dust devils", "Slayer task/Dust devils", "https://oldschool.runescape.wiki/w/Slayer_task/Dust_devils", 15319311, text);

		assertEquals(2, result.getGuide().getMethods().size());
		assertEquals(CombatMethod.MAGIC, result.getGuide().getMethods().get(0).getMethod());
		assertEquals(CombatMethod.MELEE, result.getGuide().getMethods().get(1).getMethod());
		assertEquals("Toxic blowpipe", result.getGuide().getMethods().get(0).getInventory().get(0).getItemOrCategory());
		assertEquals("Prayer potion", result.getGuide().getMethods().get(0).getInventory().get(1).getItemOrCategory());
		assertEquals(2, result.getGuide().getMethods().get(0).getInventory().get(1).getMinimumQuantity());
		assertFalse(result.getGuide().getMethods().get(1).getGear().stream().noneMatch(recommendation -> recommendation.getSlot() == GearSlot.BODY));
		assertFalse(result.getGuide().getMethods().get(1).getGear().stream().noneMatch(recommendation -> recommendation.getSlot() == GearSlot.HANDS));
		assertFalse(result.getGuide().getMethods().get(1).getGear().stream().noneMatch(recommendation -> recommendation.getSlot() == GearSlot.FEET));
	}

	@Test
	public void preservesRecommendedEquipmentTemplateRankOrder()
	{
		String text = "==Equipment==\n"
			+ "===Equipment upgrades and downgrades===\n"
			+ "{{Recommended equipment\n"
			+ "|style = Ranged\n"
			+ "|head1 = {{plink|Slayer helmet (i)}}\n"
			+ "|head2 = {{plink|Black mask (i)}}\n"
			+ "|body1 = {{plink|Masori body (f)}}\n"
			+ "|body2 = {{plink|Armadyl chestplate}}\n"
			+ "|body5 = {{plink|Black d'hide body}}\n"
			+ "|legs1 = {{plink|Masori chaps (f)}}\n"
			+ "|legs5 = {{plink|Black d'hide chaps}}\n"
			+ "|ring5 = N/A\n"
			+ "}}";

		WikiParsingResult result = new WikiStrategyParser().parse("Basilisk Knight", "Basilisk Knight/Strategies", "https://oldschool.runescape.wiki/w/Basilisk_Knight/Strategies", 789, text);

		assertEquals("Slayer helmet (i)", result.getGuide().getMethods().get(0).getGear().get(0).getTiers().get(0).getAlternatives().get(0).getName());
		assertEquals(1, result.getGuide().getMethods().get(0).getGear().get(0).getTiers().get(0).getPriority());
		assertEquals("Masori body (f)", result.getGuide().getMethods().get(0).getGear().get(1).getTiers().get(0).getAlternatives().get(0).getName());
		assertEquals("Black d'hide body", result.getGuide().getMethods().get(0).getGear().get(1).getTiers().get(2).getAlternatives().get(0).getName());
		assertEquals(5, result.getGuide().getMethods().get(0).getGear().get(1).getTiers().get(2).getPriority());
	}

	@Test
	public void renderedEquipmentAndInventoryBecomeMaxSetup()
	{
		String text = "# Ranged equipment\n"
			+ "Weapon: Dragon crossbow > Rune crossbow\n"
			+ "Body: Black d'hide body\n"
			+ "# Inventory\n"
			+ "* Prayer potion";
		String html = "<div class=\"tabbertab\" data-title=\"Ranged \">"
			+ "<table class=\"equipment equipment-right\"><tbody><tr><td>"
			+ "<div class=\"equipment-div\">"
			+ "<div class=\"equipment-head equipment-blank\"><div><a href=\"/w/Slayer_helmet_(i)\" title=\"Slayer helmet (i)\"></a></div></div>"
			+ "<div class=\"equipment-body equipment-blank\"><div><a href=\"/w/Masori_body_(f)\" title=\"Masori body (f)\"></a></div></div>"
			+ "<div class=\"equipment-weapon equipment-blank\"><div><a href=\"/w/Zaryte_crossbow\" title=\"Zaryte crossbow\"></a></div></div>"
			+ "</div></td></tr></tbody></table>"
			+ "<table class=\"inventorytable storage-right\"><tbody><tr><td><a href=\"/w/Divine_ranging_potion(4)\" title=\"Divine ranging potion(4)\"></a></td>"
			+ "<td><a href=\"/w/Cooked_karambwan\" title=\"Cooked karambwan\"></a></td></tr></tbody></table>"
			+ "</div></div>";

		WikiParsingResult source = new WikiStrategyParser().parse("Basilisk Knight", "Basilisk Knight/Strategies", "https://oldschool.runescape.wiki/w/Basilisk_Knight/Strategies", 789, text);
		WikiParsingResult result = new WikiStrategyParser().mergeRenderedHtml(source, html);

		assertEquals("Slayer helmet (i)", result.getGuide().getMethods().get(0).getGear().get(0).getTiers().get(0).getAlternatives().get(0).getName());
		assertEquals(0, result.getGuide().getMethods().get(0).getGear().get(0).getTiers().get(0).getPriority());
		GearRecommendation body = result.getGuide().getMethods().get(0).getGear().stream()
			.filter(recommendation -> recommendation.getSlot() == GearSlot.BODY)
			.findFirst()
			.get();
		assertEquals("Masori body (f)", body.getTiers().get(0).getAlternatives().get(0).getName());
		assertEquals("Divine ranging potion(4)", result.getGuide().getMethods().get(0).getInventory().get(0).getItemOrCategory());
		assertEquals("Cooked karambwan", result.getGuide().getMethods().get(0).getInventory().get(1).getItemOrCategory());
	}

	@Test
	public void renderedEquipmentDoesNotLeakLaterItemsIntoBlankSlots()
	{
		String text = "# Melee equipment\n"
			+ "Weapon: Abyssal whip";
		String html = "<div class=\"tabbertab\" data-title=\"Melee\">"
			+ "<table class=\"equipment equipment-right\"><tbody><tr><td>"
			+ "<div class=\"equipment-div\">"
			+ "<div class=\"equipment-legs equipment-blank\"><div><a href=\"/w/Legs_slot\" title=\"Legs slot\"></a></div></div>"
			+ "<div class=\"equipment-hands equipment-blank\"><div><a href=\"/w/Ferocious_gloves\" title=\"Ferocious gloves\"></a></div></div>"
			+ "</div></td></tr></tbody></table>"
			+ "</div></div>";

		WikiParsingResult source = new WikiStrategyParser().parse("Basilisk Knight", "Basilisk Knight/Strategies", "https://oldschool.runescape.wiki/w/Basilisk_Knight/Strategies", 789, text);
		WikiParsingResult result = new WikiStrategyParser().mergeRenderedHtml(source, html);

		assertFalse(result.getGuide().getMethods().get(0).getGear().stream().anyMatch(recommendation -> recommendation.getSlot() == GearSlot.LEGS));
		GearRecommendation hands = result.getGuide().getMethods().get(0).getGear().stream()
			.filter(recommendation -> recommendation.getSlot() == GearSlot.HANDS)
			.findFirst()
			.get();
		assertEquals("Ferocious gloves", hands.getTiers().get(0).getAlternatives().get(0).getName());
	}

	@Test
	public void renderedEquipmentIgnoresSlotPlaceholders()
	{
		String text = "# Melee equipment\n"
			+ "Weapon: Abyssal whip";
		String html = "<div class=\"tabbertab\" data-title=\"Melee\">"
			+ "<table class=\"equipment equipment-right\"><tbody><tr><td>"
			+ "<div class=\"equipment-div\">"
			+ "<div class=\"equipment-cape equipment-blank\"><div><a href=\"/w/Cape_slot\" title=\"Cape slot\"></a></div></div>"
			+ "<div class=\"equipment-weapon equipment-blank\"><div><a href=\"/w/File:Placeholder.png\" title=\"Placeholder\"></a><a href=\"/w/Zamorakian_hasta\" title=\"Zamorakian hasta\"></a></div></div>"
			+ "</div></td></tr></tbody></table>"
			+ "</div></div>";

		WikiParsingResult source = new WikiStrategyParser().parse("Basilisk Knight", "Basilisk Knight/Strategies", "https://oldschool.runescape.wiki/w/Basilisk_Knight/Strategies", 789, text);
		WikiParsingResult result = new WikiStrategyParser().mergeRenderedHtml(source, html);

		assertFalse(result.getGuide().getMethods().get(0).getGear().stream().anyMatch(recommendation -> recommendation.getSlot() == GearSlot.CAPE));
		GearRecommendation weapon = result.getGuide().getMethods().get(0).getGear().stream()
			.filter(recommendation -> recommendation.getSlot() == GearSlot.WEAPON)
			.findFirst()
			.get();
		assertEquals(1, weapon.getTiers().get(0).getAlternatives().size());
		assertEquals("Zamorakian hasta", weapon.getTiers().get(0).getAlternatives().get(0).getName());
	}

	@Test
	public void renderedEquipmentParsesImageOnlyWikiSlots()
	{
		String text = "# Ranged equipment\n"
			+ "Weapon: Rune crossbow";
		String html = "<div class=\"tabbertab\" data-title=\"Ranged\">"
			+ "<div class=\"equipment-div\">"
			+ "<div class=\"equipment-head equipment-blank\"><div class=\"equipment-plinkp\"><span>![](/images/Void_ranger_helm.png?75a26)</span></div></div>"
			+ "<div class=\"equipment-cape equipment-blank\"><div class=\"equipment-plinkp\"><span>![](/images/Dizana%27s_quiver.png?f8a45)</span></div></div>"
			+ "<div class=\"equipment-neck equipment-blank\"><div class=\"equipment-plinkp\"><span>![](/images/Salve_amulet%28ei%29.png?a8fa6)</span></div></div>"
			+ "<div class=\"equipment-ammo equipment-blank\"><div class=\"equipment-plinkp\"><span>![](/images/Ruby_dragon_bolts_%28e%29_5.png?8d2f7)</span></div></div>"
			+ "<div class=\"equipment-ammo2\"><div class=\"equipment-plinkp\"></div></div>"
			+ "<div class=\"equipment-weapon equipment-blank\"><div class=\"equipment-plinkp\"><span>![](/images/Dragon_hunter_crossbow.png?cc5f3)</span></div></div>"
			+ "<div class=\"equipment-torso equipment-blank\"><div class=\"equipment-plinkp\"><span>![](/images/Elite_void_top.png?7a7c0)</span></div></div>"
			+ "<div class=\"equipment-shield equipment-blank\"><div class=\"equipment-plinkp\"><span>![](/images/Dragonfire_ward.png?cc5f3)</span></div></div>"
			+ "<div class=\"equipment-legs equipment-blank\"><div class=\"equipment-plinkp\"><span>![](/images/Elite_void_robe.png?090e3)</span></div></div>"
			+ "<div class=\"equipment-gloves equipment-blank\"><div class=\"equipment-plinkp\"><span>![](/images/Void_knight_gloves.png?75a26)</span></div></div>"
			+ "<div class=\"equipment-boots equipment-blank\"><div class=\"equipment-plinkp\"><span>![](/images/Avernic_treads_%28max%29.png?301a3)</span></div></div>"
			+ "<div class=\"equipment-ring equipment-blank\"><div class=\"equipment-plinkp\"><span>![](/images/Lightbearer.png?6da96)</span></div></div>"
			+ "</div></div>";

		WikiParsingResult source = new WikiStrategyParser().parse("Vorkath", "Vorkath/Strategies", "https://oldschool.runescape.wiki/w/Vorkath/Strategies", 789, text);
		WikiParsingResult result = new WikiStrategyParser().mergeRenderedHtml(source, html);

		assertEquals("Void ranger helm", result.getGuide().getMethods().get(0).getGear().get(0).getTiers().get(0).getAlternatives().get(0).getName());
		assertEquals("Dizana's quiver", result.getGuide().getMethods().get(0).getGear().get(1).getTiers().get(0).getAlternatives().get(0).getName());
		assertEquals("Salve amulet(ei)", result.getGuide().getMethods().get(0).getGear().get(2).getTiers().get(0).getAlternatives().get(0).getName());
		assertEquals("Ruby dragon bolts (e)", result.getGuide().getMethods().get(0).getGear().get(3).getTiers().get(0).getAlternatives().get(0).getName());
		assertEquals("Dragon hunter crossbow", result.getGuide().getMethods().get(0).getGear().get(4).getTiers().get(0).getAlternatives().get(0).getName());
		assertEquals("Elite void top", result.getGuide().getMethods().get(0).getGear().get(5).getTiers().get(0).getAlternatives().get(0).getName());
		assertEquals("Void knight gloves", result.getGuide().getMethods().get(0).getGear().get(8).getTiers().get(0).getAlternatives().get(0).getName());
		assertEquals("Avernic treads (max)", result.getGuide().getMethods().get(0).getGear().get(9).getTiers().get(0).getAlternatives().get(0).getName());
	}
}
