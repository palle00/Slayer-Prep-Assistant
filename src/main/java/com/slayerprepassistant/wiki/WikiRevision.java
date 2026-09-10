package com.slayerprepassistant.wiki;

import java.time.Instant;
import java.util.Objects;

public class WikiRevision
{
	private final String title;
	private final long revisionId;
	private final Instant fetchedAt;
	private final String text;

	public WikiRevision(String title, long revisionId, Instant fetchedAt, String text)
	{
		this.title = title == null ? "" : title;
		this.revisionId = revisionId;
		this.fetchedAt = fetchedAt == null ? Instant.now() : fetchedAt;
		this.text = text == null ? "" : text;
	}

	public String getTitle()
	{
		return title;
	}

	public long getRevisionId()
	{
		return revisionId;
	}

	public String getText()
	{
		return text;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		WikiRevision that = (WikiRevision) o;
		return revisionId == that.revisionId &&
				title.equals(that.title) &&
				fetchedAt.equals(that.fetchedAt) &&
				text.equals(that.text);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(title, revisionId, fetchedAt, text);
	}

	@Override
	public String toString()
	{
		return "WikiRevision{title='" + title + "', revisionId=" + revisionId + "}";
	}
}