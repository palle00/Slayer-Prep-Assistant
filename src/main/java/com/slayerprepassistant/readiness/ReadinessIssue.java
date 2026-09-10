package com.slayerprepassistant.readiness;

public class ReadinessIssue
{
	private final String severity;
	private final String message;

	public ReadinessIssue(String severity, String message)
	{
		this.severity = severity == null || severity.trim().isEmpty() ? "INFO" : severity;
		this.message = message == null ? "" : message;
	}


}