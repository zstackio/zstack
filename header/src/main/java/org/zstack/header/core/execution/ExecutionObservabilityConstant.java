package org.zstack.header.core.execution;

/** Service identifiers used by the execution observability module. */
public interface ExecutionObservabilityConstant {
    String SERVICE_ID = "observability";

    String STATE_SUCCEEDED = "SUCCEEDED";
    String STATE_FAILED = "FAILED";
    String STATE_TIMEOUT = "TIMEOUT";
    String STATE_CANCELLED = "CANCELLED";
}
