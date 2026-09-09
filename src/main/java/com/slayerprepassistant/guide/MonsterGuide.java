package com.slayerprepassistant.guide;

import com.slayerprepassistant.model.ParsingConfidence;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class MonsterGuide
{
	private final String monsterName;
	private final String wikiTitle;
	private final String wikiUrl;
	private final long revisionId;
	private final Instant fetchedAt;
	private final List<StrategyMethod> methods;
	private final List<String> notes;
	private final ParsingConfidence confidence;

	public MonsterGuide(String monsterName, String wikiTitle, String wikiUrl, long revisionId, Instant fetchedAt, List<StrategyMethod> methods, List<String> notes, ParsingConfidence confidence)
	{
		this.monsterName = monsterName;
		this.wikiTitle = wikiTitle;
		this.wikiUrl = wikiUrl;
		this.revisionId = revisionId;
		this.fetchedAt = fetchedAt;
		this.methods = Collections.unmodifiableList(methods);
		this.notes = Collections.unmodifiableList(notes);
		this.confidence = confidence;
	}

	public String getMonsterName()
	{
		return monsterName;
	}

	public String getWikiTitle()
	{
		return wikiTitle;
	}

	public String getWikiUrl()
	{
		return wikiUrl;
	}

	public long getRevisionId()
	{
		return revisionId;
	}

	public Instant getFetchedAt()
	{
		return fetchedAt;
	}

	public List<StrategyMethod> getMethods()
	{
		return methods;
	}

	public List<String> getNotes()
	{
		return notes;
	}

	public ParsingConfidence getConfidence()
	{
		return confidence;
	}
}
