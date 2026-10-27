package org.zstack.kvm.memory;

import org.zstack.kvm.KVMAgentCommands;
import java.util.Map;

/** The memory Agent reply; hostUuid is an outer, not state, field. */
public class MemoryAgentResponse extends KVMAgentCommands.AgentResponse {
    public String operationUuid;
    public String hostUuid;
    /** Succeeded, Converging (resend after retryAfterSeconds) or Failed. */
    public String status;
    public String reason;
    public String reasonCode;
    public Long retryAfterSeconds;
    public Long generation;
    public Long appliedGeneration;
    public Map<String, Object> state;
}
