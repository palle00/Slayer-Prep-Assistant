package com.slayerprepassistant.guide;

import com.slayerprepassistant.model.ParsingConfidence;
import java.time.Instant;
import java.util.ArrayList;
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
		this.monsterName = monsterName == null ? "" : monsterName;
		this.wikiTitle = wikiTitle == null ? "" : wikiTitle;
		this.wikiUrl = wikiUrl == null ? "" : wikiUrl;
		this.revisionId = revisionId;
		this.fetchedAt = fetchedAt == null ? Instant.EPOCH : fetchedAt;
		this.methods = methods == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(methods));
		this.notes = notes == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(notes));
		this.confidence = confidence == null ? ParsingConfidence.UNKNOWN : confidence;
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