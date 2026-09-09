package com.slayerprepassistant.wiki;

import com.slayerprepassistant.guide.MonsterGuide;
import java.util.Collections;
import java.util.List;

public class WikiParsingResult
{
	private final MonsterGuide guide;
	private final List<MonsterVariant> variants;
	private final List<String> warnings;

	public WikiParsingResult(MonsterGuide guide, List<String> warnings)
	{
		this(guide, Collections.emptyList(), warnings);
	}

	public WikiParsingResult(MonsterGuide guide, List<MonsterVariant> variants, List<String> warnings)
	{
		this.guide = guide;
		this.variants = Collections.unmodifiableList(variants);
		this.warnings = Collections.unmodifiableList(warnings);
	}

	public MonsterGuide getGuide()
	{
		return guide;
	}

	public List<MonsterVariant> getVariants()
	{
		return variants;
	}

	public List<String> getWarnings()
	{
		return warnings;
	}
}
