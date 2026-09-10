package com.slayerprepassistant.wiki;

import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class WikiImageService
{
	private final WikiClient wikiClient;
	private final BooleanSupplier enabled;

	public WikiImageService(WikiClient wikiClient, BooleanSupplier enabled)
	{
		this.wikiClient = Objects.requireNonNull(wikiClient, "wikiClient cannot be null");
		this.enabled = Objects.requireNonNull(enabled, "enabled cannot be null");
	}

	public void load(String title, int size, Consumer<BufferedImage> callback)
	{
		if (callback == null)
		{
			return;
		}
		if (!enabled.getAsBoolean() || title == null || title.trim().isEmpty() || size <= 0)
		{
			callback.accept(null);
			return;
		}
		wikiClient.fetchPageImage(title.trim(), size, callback);
	}
}
