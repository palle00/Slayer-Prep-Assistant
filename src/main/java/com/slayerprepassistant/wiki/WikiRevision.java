package com.slayerprepassistant.wiki;

import java.time.Instant;

public class WikiRevision
{
	private final String title;
	private final long revisionId;
	private final Instant fetchedAt;
	private final String text;

	public WikiRevision(String title, long revisionId, Instant fetchedAt, String text)
	{
		this.title = title;
		this.revisionId = revisionId;
		this.fetchedAt = fetchedAt;
		this.text = text;
	}

	public String getTitle()
	{
		return title;
	}

	public long getRevisionId()
	{
		return revisionId;
	}

	public Instant getFetchedAt()
	{
		return fetchedAt;
	}

	public String getText()
	{
		return text;
	}
}
