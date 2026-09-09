package com.slayerprepassistant.items;

import com.slayerprepassistant.gear.RecommendedItem;
import java.util.Locale;
import java.util.Set;

public class ItemResolver
{
	public RecommendedItem resolve(String wikiName)
	{
		return new RecommendedItem(wikiName);
	}

	public boolean matches(String recommendedName, Set<String> ownedNames)
	{
		String canonicalRecommended = canonical(normalize(recommendedName));
		for (String ownedName : ownedNames)
		{
			if (canonical(normalize(ownedName)).equals(canonicalRecommended))
			{
				return true;
			}
		}
		return false;
	}

	public static String normalize(String value)
	{
		return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
	}

	private String canonical(String normalized)
	{
		String canonical = normalized
			.replace(" helm ", " helmet ")
			.replaceFirst("^helm ", "helmet ")
			.replaceFirst(" helm$", " helmet")
			.replaceFirst("^imbued ", "")
			.replaceFirst(" i$", "")
			.replaceFirst(" loaded$", "")
			.replaceFirst(" empty$", "")
			.replaceFirst(" ornament$", "")
			.replaceFirst(" ornament kit$", "");
		return isDoseOrChargeVariant(canonical) ? canonical.replaceFirst(" [0-9]+$", "") : canonical;
	}

	private boolean isDoseOrChargeVariant(String normalized)
	{
		return normalized.matches(".*\\b(potion|brew|restore|serum|mix|antipoison|antidote|antifire|venom|stamina|sanfew|overload|black mask|slayer ring|ring of dueling|games necklace|combat bracelet|skills necklace|amulet of glory)\\b.* [0-9]+$");
	}
}
