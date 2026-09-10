package com.slayerprepassistant.wiki;

import java.util.Objects;

public class WikiResponse
{
	private final WikiState state;
	private final WikiRevision revision;
	private final String message;

	public WikiResponse(WikiState state, WikiRevision revision, String message)
	{
		this.state = Objects.requireNonNullElse(state, WikiState.ERROR);
		this.revision = revision;
		this.message = message == null ? "" : message;
	}

	public WikiState getState()
	{
		return state;
	}

	public WikiRevision getRevision()
	{
		return revision;
	}

	public String getMessage()
	{
		return message;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		WikiResponse that = (WikiResponse) o;
		return state == that.state &&
				Objects.equals(revision, that.revision) &&
				message.equals(that.message);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(state, revision, message);
	}

	@Override
	public String toString()
	{
		return "WikiResponse{state=" + state + ", message='" + message + "'}";
	}
}