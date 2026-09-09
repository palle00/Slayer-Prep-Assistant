package com.slayerprepassistant.gear;

import java.util.Collections;
import java.util.List;

public class LoadoutResult
{
	private final LoadoutMode mode;
	private final List<GearMatch> gearMatches;

	public LoadoutResult(LoadoutMode mode, List<GearMatch> gearMatches)
	{
		this.mode = mode;
		this.gearMatches = Collections.unmodifiableList(gearMatches);
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
