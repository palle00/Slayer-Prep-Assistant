package com.slayerprepassistant.wiki;

public class WikiResponse
{
	private final WikiState state;
	private final WikiRevision revision;
	private final String message;

	public WikiResponse(WikiState state, WikiRevision revision, String message)
	{
		this.state = state;
		this.revision = revision;
		this.message = message;
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
}
