package org.zstack.sdk;

public enum AiHostModelCacheFailureCode {
	JuicefsMountFailed,
	JuicefsWarmupFailed,
	InsufficientHostCacheStorage,
	SourcePathInvalid,
	PermissionDenied,
	AgentTimeout,
	Unknown,
}
