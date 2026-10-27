package org.zstack.kvm.memory;

/** A transport ACK is not an application result; the reply must answer the operation. */
public final class MemoryResultRules {
    private MemoryResultRules() { }
    public static boolean succeeded(String operation, MemoryAgentResponse response) {
        return response != null && response.isSuccess() && operation.equals(response.operationUuid)
                && "Succeeded".equals(response.status);
    }
}
