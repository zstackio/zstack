package org.zstack.kvm.memory;

import org.zstack.kvm.KVMAgentCommands;
import java.util.Map;

/** Agent envelope fields used by MN projections; hostUuid is an outer, not state, field. */
public class MemoryAgentResponse extends KVMAgentCommands.AgentResponse {
    public String operationUuid;
    public String bootId;
    public String hostUuid;
    public String status;
    public String reason;
    public String reasonCode;
    public Map<String, Object> capabilities;
    public Map<String, Object> state;
    public Long appliedRevision;
    public Long sampleTime;
}
