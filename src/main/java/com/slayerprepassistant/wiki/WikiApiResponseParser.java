package com.slayerprepassistant.wiki;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.time.Instant;

class WikiApiResponseParser
{
	private final Gson gson;

	WikiApiResponseParser(Gson gson)
	{
		this.gson = gson;
	}

	WikiResponse parseRevision(String fallbackTitle, String json)
	{
		try
		{
			JsonObject root = gson.fromJson(json, JsonObject.class);
			JsonObject query = root.getAsJsonObject("query");
			JsonObject pages = query == null ? null : query.getAsJsonObject("pages");
			if (pages == null || pages.entrySet().isEmpty())
			{
				return new WikiResponse(WikiState.ERROR, null, "Wiki page not found");
			}
			JsonObject page = pages.entrySet().iterator().next().getValue().getAsJsonObject();
			if (page.has("missing"))
			{
				return new WikiResponse(WikiState.ERROR, null, "Wiki page not found");
			}
			String title = page.has("title") ? page.get("title").getAsString() : fallbackTitle;
			JsonArray revisions = page.getAsJsonArray("revisions");
			if (revisions == null || revisions.size() == 0)
			{
				return new WikiResponse(WikiState.ERROR, null, "Wiki revision not found");
			}
			JsonObject revision = revisions.get(0).getAsJsonObject();
			long revisionId = revision.has("revid") ? revision.get("revid").getAsLong() : 0;
			String text = revisionText(revision);
			return new WikiResponse(WikiState.READY, new WikiRevision(title, revisionId, Instant.now(), text), "");
		}
		catch (RuntimeException ex)
		{
			return new WikiResponse(WikiState.ERROR, null, "Unable to parse Wiki response");
		}
	}

	WikiResponse parseRendered(String fallbackTitle, String json)
	{
		try
		{
			JsonObject root = gson.fromJson(json, JsonObject.class);
			JsonObject parse = root.getAsJsonObject("parse");
			if (parse == null)
			{
				return new WikiResponse(WikiState.ERROR, null, "Wiki page not found");
			}
			String title = parse.has("title") ? parse.get("title").getAsString() : fallbackTitle;
			long revisionId = parse.has("revid") ? parse.get("revid").getAsLong() : 0;
			JsonObject text = parse.getAsJsonObject("text");
			JsonElement html = text == null ? null : text.get("*");
			return html == null
				? new WikiResponse(WikiState.ERROR, null, "Wiki rendered HTML not found")
				: new WikiResponse(WikiState.READY, new WikiRevision(title, revisionId, Instant.now(), html.getAsString()), "");
		}
		catch (RuntimeException ex)
		{
			return new WikiResponse(WikiState.ERROR, null, "Unable to parse Wiki response");
		}
	}

	String pageImageUrl(String json)
	{
		try
		{
			JsonObject root = gson.fromJson(json, JsonObject.class);
			JsonObject query = root.getAsJsonObject("query");
			JsonObject pages = query == null ? null : query.getAsJsonObject("pages");
			if (pages == null || pages.entrySet().isEmpty())
			{
				return "";
			}
			JsonObject page = pages.entrySet().iterator().next().getValue().getAsJsonObject();
			JsonObject thumbnail = page.getAsJsonObject("thumbnail");
			return thumbnail != null && thumbnail.has("source") ? thumbnail.get("source").getAsString() : "";
		}
		catch (RuntimeException ex)
		{
			return "";
		}
	}

	private String revisionText(JsonObject revision)
	{
		JsonObject slots = revision.getAsJsonObject("slots");
		JsonObject main = slots == null ? null : slots.getAsJsonObject("main");
		JsonElement text = main == null ? revision.get("*") : main.get("*");
		if (text == null && main != null)
		{
			text = main.get("content");
		}
		return text == null ? "" : text.getAsString();
	}
}
