package com.slayerprepassistant.wiki;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class WikiApiResponseParser
{
	private final Gson gson;

	WikiApiResponseParser(Gson gson)
	{
		this.gson = gson;
	}

	WikiResponse parseRevision(String fallbackTitle, String json)
	{
		Map<String, WikiResponse> responses = parseRevisions(Collections.singletonList(fallbackTitle), json);
		return responses.getOrDefault(
			WikiTitles.cacheKey(fallbackTitle),
			new WikiResponse(WikiState.ERROR, null, "Unable to parse Wiki response"));
	}

	Map<String, WikiResponse> parseRevisions(List<String> requestedTitles, String json)
	{
		Map<String, WikiResponse> result = new LinkedHashMap<>();
		if (requestedTitles == null || requestedTitles.isEmpty())
		{
			return result;
		}
		try
		{
			JsonObject root = gson.fromJson(json, JsonObject.class);
			JsonObject query = root == null ? null : root.getAsJsonObject("query");
			JsonObject pages = query == null ? null : query.getAsJsonObject("pages");
			if (pages == null)
			{
				return errorsFor(requestedTitles, "Wiki page not found");
			}

			Map<String, String> aliases = new HashMap<>();
			collectAliases(query, "normalized", aliases);
			collectAliases(query, "redirects", aliases);

			Map<String, WikiResponse> responsesByTitle = new HashMap<>();
			for (Map.Entry<String, JsonElement> entry : pages.entrySet())
			{
				JsonObject page = entry.getValue() == null || !entry.getValue().isJsonObject()
					? null
					: entry.getValue().getAsJsonObject();
				if (page == null)
				{
					continue;
				}
				String title = page.has("title") ? page.get("title").getAsString() : "";
				responsesByTitle.put(WikiTitles.cacheKey(title), parseRevisionPage(title, page));
			}

			for (String requestedTitle : requestedTitles)
			{
				String requestedKey = WikiTitles.cacheKey(requestedTitle);
				String resolvedKey = resolveAlias(requestedKey, aliases);
				WikiResponse response = responsesByTitle.get(resolvedKey);
				if (response == null)
				{
					response = responsesByTitle.get(requestedKey);
				}
				result.put(requestedKey, response == null
					? new WikiResponse(WikiState.ERROR, null, "Wiki page not found")
					: response);
			}
			return result;
		}
		catch (RuntimeException ex)
		{
			return errorsFor(requestedTitles, "Unable to parse Wiki response");
		}
	}

	WikiResponse parseRendered(String fallbackTitle, String json)
	{
		try
		{
			JsonObject root = gson.fromJson(json, JsonObject.class);
			JsonObject parse = root == null ? null : root.getAsJsonObject("parse");
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

	String pageImageUrl(String title, String json)
	{
		return pageImageUrls(Collections.singletonList(title), json).getOrDefault(WikiTitles.cacheKey(title), "");
	}

	Map<String, String> pageImageUrls(List<String> requestedTitles, String json)
	{
		Map<String, String> result = new LinkedHashMap<>();
		if (requestedTitles == null || requestedTitles.isEmpty())
		{
			return result;
		}
		try
		{
			JsonObject root = gson.fromJson(json, JsonObject.class);
			JsonObject query = root == null ? null : root.getAsJsonObject("query");
			JsonObject pages = query == null ? null : query.getAsJsonObject("pages");
			if (pages == null)
			{
				return emptyImageResults(requestedTitles);
			}

			Map<String, String> aliases = new HashMap<>();
			collectAliases(query, "normalized", aliases);
			collectAliases(query, "redirects", aliases);
			Map<String, String> imagesByTitle = new HashMap<>();
			for (Map.Entry<String, JsonElement> entry : pages.entrySet())
			{
				JsonElement value = entry.getValue();
				if (value == null || !value.isJsonObject())
				{
					continue;
				}
				JsonObject page = value.getAsJsonObject();
				String pageTitle = page.has("title") ? page.get("title").getAsString() : "";
				JsonObject thumbnail = page.getAsJsonObject("thumbnail");
				String imageUrl = thumbnail != null && thumbnail.has("source") ? thumbnail.get("source").getAsString() : "";
				imagesByTitle.put(WikiTitles.cacheKey(pageTitle), imageUrl);
			}

			for (String requestedTitle : requestedTitles)
			{
				String requestedKey = WikiTitles.cacheKey(requestedTitle);
				String resolvedKey = resolveAlias(requestedKey, aliases);
				result.put(requestedKey, imagesByTitle.getOrDefault(resolvedKey, imagesByTitle.getOrDefault(requestedKey, "")));
			}
			return result;
		}
		catch (RuntimeException ex)
		{
			return emptyImageResults(requestedTitles);
		}
	}

	private Map<String, String> emptyImageResults(List<String> requestedTitles)
	{
		Map<String, String> empty = new LinkedHashMap<>();
		for (String title : requestedTitles)
		{
			empty.put(WikiTitles.cacheKey(title), "");
		}
		return empty;
	}

	private WikiResponse parseRevisionPage(String fallbackTitle, JsonObject page)
	{
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
		return new WikiResponse(WikiState.READY, new WikiRevision(title, revisionId, Instant.now(), revisionText(revision)), "");
	}

	private void collectAliases(JsonObject query, String member, Map<String, String> aliases)
	{
		JsonArray array = query.getAsJsonArray(member);
		if (array == null)
		{
			return;
		}
		for (JsonElement element : array)
		{
			if (element == null || !element.isJsonObject())
			{
				continue;
			}
			JsonObject alias = element.getAsJsonObject();
			if (alias.has("from") && alias.has("to"))
			{
				aliases.put(WikiTitles.cacheKey(alias.get("from").getAsString()), WikiTitles.cacheKey(alias.get("to").getAsString()));
			}
		}
	}

	private String resolveAlias(String key, Map<String, String> aliases)
	{
		String current = key;
		for (int i = 0; i <= aliases.size(); i++)
		{
			String next = aliases.get(current);
			if (next == null || next.equals(current))
			{
				return current;
			}
			current = next;
		}
		return current;
	}

	private Map<String, WikiResponse> errorsFor(List<String> requestedTitles, String message)
	{
		Map<String, WikiResponse> errors = new LinkedHashMap<>();
		for (String title : requestedTitles)
		{
			errors.put(WikiTitles.cacheKey(title), new WikiResponse(WikiState.ERROR, null, message));
		}
		return errors;
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
