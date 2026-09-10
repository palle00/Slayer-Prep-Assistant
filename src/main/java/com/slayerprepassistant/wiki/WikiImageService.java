package com.slayerprepassistant.wiki;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class WikiImageService
{
	private final WikiClient wikiClient;
	private final BooleanSupplier enabled;
	private final Map<String, BufferedImage> imageCache = new HashMap<>();

	public WikiImageService(WikiClient wikiClient, BooleanSupplier enabled)
	{
		this.wikiClient = wikiClient;
		this.enabled = enabled;
	}

	public void load(String title, int size, Consumer<BufferedImage> callback)
	{
		if (!enabled.getAsBoolean() || title == null || title.trim().isEmpty())
		{
			callback.accept(null);
			return;
		}
		String key = title.trim().toLowerCase(Locale.ROOT) + "|" + size;
		BufferedImage cached = imageCache.get(key);
		if (cached != null)
		{
			callback.accept(cached);
			return;
		}
		wikiClient.fetchPageImage(title, size, image ->
		{
			if (image != null)
			{
				imageCache.put(key, image);
			}
			callback.accept(image);
		});
	}
}
