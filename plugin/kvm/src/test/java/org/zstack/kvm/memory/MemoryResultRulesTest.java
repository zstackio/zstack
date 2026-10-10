package org.zstack.kvm.memory;

import org.junit.Test;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

public class MemoryResultRulesTest {
    private String status(String operation, long revision, MemoryAgentResponse response) throws Exception {
        Class<?> rules;
        try { rules = Class.forName("org.zstack.kvm.memory.MemoryResultRules"); }
        catch (ClassNotFoundException e) { throw new AssertionError("Result fencing is not implemented", e); }
        Method method = rules.getMethod("status", String.class, long.class, MemoryAgentResponse.class);
        return (String) method.invoke(null, operation, revision, response);
    }

    @Test public void httpSuccessDoesNotProvePolicyApplied() throws Exception {
        MemoryAgentResponse response = new MemoryAgentResponse();
        response.setSuccess(true); response.status = "Succeeded";
        assertEquals("Unknown", status("op", 7, response));
        response.operationUuid = "op"; response.appliedRevision = 6L;
        assertEquals("Unknown", status("op", 7, response));
        response.appliedRevision = 7L;
        assertEquals("Succeeded", status("op", 7, response));
        response.operationUuid = "other";
        assertEquals("Unknown", status("op", 7, response));
    }

    @Test public void drainingAndExplicitFailureAreNotSuccess() throws Exception {
        MemoryAgentResponse response = new MemoryAgentResponse();
        response.operationUuid = "op"; response.status = "Draining";
        response.setSuccess(true);
        assertEquals("Draining", status("op", 9, response));
        response.status = "Failed"; response.setSuccess(false);
        assertEquals("Failed", status("op", 9, response));
        response.status = "UNKNOWN";
        assertEquals("Unknown", status("op", 9, response));
    }

    @Test public void knownBlockedIsExplicitAndNeverAnAppliedRevision() throws Exception {
        MemoryAgentResponse response = new MemoryAgentResponse();
        response.operationUuid = "op"; response.status = "Blocked"; response.setSuccess(false);
        response.state = new java.util.LinkedHashMap<>();
        response.state.put("phase", "BLOCKED"); response.state.put("knownBlocked", true);
        java.util.Map<String, Object> blocker = new java.util.LinkedHashMap<>();
        blocker.put("section", "capacity"); blocker.put("code", "NATIVE_POOL_UNQUALIFIED");
        response.state.put("blockers", java.util.Collections.singletonList(blocker));
        assertEquals("Blocked", status("op", 9, response));
        assertEquals("Unknown", status("other", 9, response));
        response.appliedRevision = 9L;
        assertEquals("Unknown", status("op", 9, response));
        response.appliedRevision = null; blocker.put("code", "arbitrary stderr");
        assertEquals("Unknown", status("op", 9, response));
        blocker.put("code", "NATIVE_POOL_UNQUALIFIED"); response.setSuccess(true);
        assertEquals("Unknown", status("op", 9, response));
        response.setSuccess(false); response.state.remove("knownBlocked");
        assertEquals("Unknown", status("op", 9, response));
    }
}
