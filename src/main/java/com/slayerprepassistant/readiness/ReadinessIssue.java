package com.slayerprepassistant.readiness;

public class ReadinessIssue
{
	private final String severity;
	private final String message;

	public ReadinessIssue(String severity, String message)
	{
		this.severity = severity;
		this.message = message;
	}

	public String getSeverity()
	{
		return severity;
	}

	public String getMessage()
	{
		return message;
	}
}
