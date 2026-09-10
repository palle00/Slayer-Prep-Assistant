package com.slayerprepassistant.gear;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LoadoutResult
{
	private final LoadoutMode mode;
	private final List<GearMatch> gearMatches;

	public LoadoutResult(LoadoutMode mode, List<GearMatch> gearMatches)
	{
		this.mode = mode == null ? LoadoutMode.MAX : mode;
		this.gearMatches = gearMatches == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(gearMatches));
	}

	public LoadoutMode getMode()
	{
		return mode;
	}

	public List<GearMatch> getGearMatches()
	{
		return gearMatches;
	}
}