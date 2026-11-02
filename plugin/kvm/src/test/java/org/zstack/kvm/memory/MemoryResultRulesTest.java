package org.zstack.kvm.memory;

import org.junit.Test;
import static org.junit.Assert.*;

public class MemoryResultRulesTest {
    /** A reply the Agent call delivered, with the operation's status. */
    private static MemoryAgentResponse reply(String operation, String status) {
        MemoryAgentResponse response = new MemoryAgentResponse();
        response.operationUuid = operation; response.setSuccess(true); response.status = status;
        return response;
    }

    /** A reply whose Agent call failed, whatever status it carries. */
    private static MemoryAgentResponse failedCall(String operation, String status) {
        MemoryAgentResponse response = reply(operation, status);
        response.setSuccess(false);
        return response;
    }

    @Test public void onlyASuccessfulReplyToTheSameOperationSucceeds() {
        assertTrue(MemoryResultRules.succeeded("op", reply("op", "Succeeded")));
        assertFalse(MemoryResultRules.succeeded("op", null));
        assertFalse(MemoryResultRules.succeeded("op", reply("other", "Succeeded")));
        assertFalse(MemoryResultRules.succeeded("op", failedCall("op", "Succeeded")));
        assertFalse(MemoryResultRules.succeeded("op", reply("op", "Converging")));
        assertFalse(MemoryResultRules.succeeded("op", reply("op", null)));
    }
}
