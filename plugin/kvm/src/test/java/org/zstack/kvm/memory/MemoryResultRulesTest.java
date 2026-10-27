package org.zstack.kvm.memory;

import org.junit.Test;
import static org.junit.Assert.*;

public class MemoryResultRulesTest {
    private static MemoryAgentResponse response(String operation, boolean success, String status) {
        MemoryAgentResponse response = new MemoryAgentResponse();
        response.operationUuid = operation; response.setSuccess(success); response.status = status;
        return response;
    }

    @Test public void onlyASuccessfulReplyToTheSameOperationSucceeds() {
        assertTrue(MemoryResultRules.succeeded("op", response("op", true, "Succeeded")));
        assertFalse(MemoryResultRules.succeeded("op", null));
        assertFalse(MemoryResultRules.succeeded("op", response("other", true, "Succeeded")));
        assertFalse(MemoryResultRules.succeeded("op", response("op", false, "Succeeded")));
        assertFalse(MemoryResultRules.succeeded("op", response("op", true, "Converging")));
        assertFalse(MemoryResultRules.succeeded("op", response("op", true, null)));
    }
}
