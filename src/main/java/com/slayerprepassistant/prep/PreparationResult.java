package com.slayerprepassistant.prep;

import com.slayerprepassistant.gear.LoadoutResult;
import com.slayerprepassistant.bank.PlayerInventoryState;
import com.slayerprepassistant.guide.CombatMethod;
import com.slayerprepassistant.guide.MonsterGuide;
import com.slayerprepassistant.guide.StrategyMethod;
import com.slayerprepassistant.readiness.ReadinessResult;
import com.slayerprepassistant.task.SlayerTaskContext;
import com.slayerprepassistant.task.TargetOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PreparationResult
{
	private final PreparationStatus status;
	private final String message;
	private final SlayerTaskContext taskContext;
	private final List<TargetOption> targets;
	private final TargetOption selectedTarget;
	private final MonsterGuide guide;
	private final CombatMethod selectedMethod;
	private final StrategyMethod strategyMethod;
	private final LoadoutResult loadoutResult;
	private final ReadinessResult readinessResult;
	private final PlayerInventoryState playerState;

	public PreparationResult(SlayerTaskContext taskContext, List<TargetOption> targets, TargetOption selectedTarget, MonsterGuide guide, CombatMethod selectedMethod, StrategyMethod strategyMethod, LoadoutResult loadoutResult, ReadinessResult readinessResult, PlayerInventoryState playerState)
	{
		this(PreparationStatus.SETUP_READY, "", taskContext, targets, selectedTarget, guide, selectedMethod, strategyMethod, loadoutResult, readinessResult, playerState);
	}

	private PreparationResult(PreparationStatus status, String message, SlayerTaskContext taskContext, List<TargetOption> targets, TargetOption selectedTarget, MonsterGuide guide, CombatMethod selectedMethod, StrategyMethod strategyMethod, LoadoutResult loadoutResult, ReadinessResult readinessResult, PlayerInventoryState playerState)
	{
		this.status = status == null ? PreparationStatus.NO_SETUP : status;
		this.message = message == null ? "" : message;
		this.taskContext = taskContext;
		this.targets = targets == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(targets));
		this.selectedTarget = selectedTarget;
		this.guide = guide;
		this.selectedMethod = selectedMethod == null ? CombatMethod.defaultMethod() : selectedMethod;
		this.strategyMethod = strategyMethod;
		this.loadoutResult = loadoutResult;
		this.readinessResult = readinessResult;
		this.playerState = playerState == null ? PlayerInventoryState.unknownBank() : playerState;
	}

	public static PreparationResult noSetup(SlayerTaskContext taskContext, List<TargetOption> targets, TargetOption selectedTarget, String message)
	{
		return new PreparationResult(PreparationStatus.NO_SETUP, message, taskContext, targets, selectedTarget, null, CombatMethod.defaultMethod(), null, null, null, PlayerInventoryState.unknownBank());
	}

	public PreparationStatus getStatus()
	{
		return status;
	}

	public String getMessage()
	{
		return message;
	}
	

	public List<TargetOption> getTargets()
	{
		return targets;
	}

	public TargetOption getSelectedTarget()
	{
		return selectedTarget;
	}

	public MonsterGuide getGuide()
	{
		return guide;
	}

	public CombatMethod getSelectedMethod()
	{
		return selectedMethod;
	}

	public StrategyMethod getStrategyMethod()
	{
		return strategyMethod;
	}

	public LoadoutResult getLoadoutResult()
	{
		return loadoutResult;
	}

	public ReadinessResult getReadinessResult()
	{
		return readinessResult;
	}

	public PlayerInventoryState getPlayerState()
	{
		return playerState;
	}
}