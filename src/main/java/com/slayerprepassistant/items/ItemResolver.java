package com.slayerprepassistant.items;

import com.slayerprepassistant.gear.RecommendedItem;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class ItemResolver
{
	private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
	private static final Pattern TRAILING_NUMBER = Pattern.compile(" [0-9]+$");
	private static final Pattern DOSE_OR_CHARGE_VARIANT = Pattern.compile(
		".*\\b(potion|brew|restore|serum|mix|antipoison|antidote|antifire|venom|stamina|sanfew|overload|black mask|slayer ring|ring of dueling|games necklace|combat bracelet|skills necklace|amulet of glory)\\b.* [0-9]+$");

	public RecommendedItem resolve(String wikiName)
	{
		return new RecommendedItem(wikiName == null ? "" : wikiName);
	}

	public boolean matches(String recommendedName, Set<String> ownedNames)
	{
		if (ownedNames == null || ownedNames.isEmpty())
		{
			return false;
		}
		String canonicalRecommended = canonicalKey(recommendedName);
		if (canonicalRecommended.isEmpty())
		{
			return false;
		}
		for (String ownedName : ownedNames)
		{
			if (canonicalKey(ownedName).equals(canonicalRecommended))
			{
				return true;
			}
		}
		return false;
	}

	public static String normalize(String value)
	{
		if (value == null)
		{
			return "";
		}
		return NON_ALPHANUMERIC.matcher(value.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
	}

	public static String canonicalKey(String value)
	{
		String normalized = normalize(value);
		if (normalized.isEmpty())
		{
			return "";
		}

		String canonical = normalized.replace(" helm ", " helmet ");
		if (canonical.startsWith("helm "))
		{
			canonical = "helmet " + canonical.substring("helm ".length());
		}
		if (canonical.endsWith(" helm"))
		{
			canonical = canonical.substring(0, canonical.length() - " helm".length()) + " helmet";
		}

		canonical = removeSuffix(canonical, " i");
		canonical = removeSuffix(canonical, " loaded");
		canonical = removeSuffix(canonical, " empty");
		canonical = removeSuffix(canonical, " ornament kit");
		canonical = removeSuffix(canonical, " ornament");

		return DOSE_OR_CHARGE_VARIANT.matcher(canonical).matches()
			? TRAILING_NUMBER.matcher(canonical).replaceFirst("")
			: canonical;
	}

	public static boolean hasDifferentNumberedVariant(String recommendedName, Set<String> ownedCanonicalNames)
	{
		if (ownedCanonicalNames == null || ownedCanonicalNames.isEmpty())
		{
			return false;
		}
		String recommended = canonicalKey(recommendedName);
		String recommendedBase = numberedVariantBase(recommended);
		if (recommendedBase.equals(recommended))
		{
			return false;
		}
		for (String owned : ownedCanonicalNames)
		{
			if (owned != null && !owned.equals(recommended) && numberedVariantBase(owned).equals(recommendedBase))
			{
				return true;
			}
		}
		return false;
	}

	private static String numberedVariantBase(String canonical)
	{
		return canonical == null ? "" : TRAILING_NUMBER.matcher(canonical).replaceFirst("");
	}

	private static String removeSuffix(String value, String suffix)
	{
		return value.endsWith(suffix) ? value.substring(0, value.length() - suffix.length()).trim() : value;
	}
}
