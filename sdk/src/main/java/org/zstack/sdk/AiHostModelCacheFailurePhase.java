package org.zstack.sdk;

public enum AiHostModelCacheFailurePhase {
	ModelSourceMount,
	ModelSourceWarmup,
	PreparedSourceValidation,
	CapacityCheck,
	AgentExecution,
	Unknown,
}
