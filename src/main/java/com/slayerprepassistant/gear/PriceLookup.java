package com.slayerprepassistant.gear;

import java.util.OptionalInt;

public interface PriceLookup
{
	OptionalInt price(RecommendedItem item);

	static PriceLookup unavailable()
	{
		return item -> OptionalInt.empty();
	}
}
