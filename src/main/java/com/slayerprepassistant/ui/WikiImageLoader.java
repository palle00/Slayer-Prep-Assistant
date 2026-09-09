package com.slayerprepassistant.ui;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

@FunctionalInterface
public interface WikiImageLoader
{
	void load(String title, int size, Consumer<BufferedImage> callback);
}
