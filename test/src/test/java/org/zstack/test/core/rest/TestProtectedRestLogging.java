package org.zstack.test.core.rest;

import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.rest.RESTFacadeImpl;
import org.zstack.header.rest.AsyncRESTCallback;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class TestProtectedRestLogging {
    private static class RecordingFacade extends RESTFacadeImpl {
        String wireBody;
        String logBody;
        boolean protectedLogging;

        @Override
        protected void asyncJson(String url, String body, Map<String, String> headers, HttpMethod method,
                                 AsyncRESTCallback callback, TimeUnit unit, long timeout,
                                 String loggedBody, boolean safeLogging) {
            wireBody = body;
            logBody = loggedBody;
            protectedLogging = safeLogging;
        }
    }

    private static class UnannotatedCommand {
        String opaque = "fixture-private-command";
    }

    private void assertProtected(Object command, String secret) {
        RecordingFacade facade = new RecordingFacade();
        boolean unitTestOn = CoreGlobalProperty.UNIT_TEST_ON;
        try {
            CoreGlobalProperty.UNIT_TEST_ON = false;
            facade.asyncJsonPostWithSafeLogging("http://127.0.0.1/command", command, null, TimeUnit.SECONDS, 1);
        } finally {
            CoreGlobalProperty.UNIT_TEST_ON = unitTestOn;
        }
        assertTrue("agent must still receive the command", facade.wireBody.contains(secret));
        assertNotNull(facade.logBody);
        assertFalse("timeouts and TRACE must not expose unannotated payloads", facade.logBody.contains(secret));
        assertTrue(facade.protectedLogging);
    }

    @Test
    public void mapPayloadIsProtectedWithoutChangingTheWireRequest() {
        assertProtected(Collections.singletonMap("password", "fixture-map-password"), "fixture-map-password");
    }

    @Test
    public void unannotatedCommandIsProtectedWithoutRelyingOnCredentialFieldNames() {
        assertProtected(new UnannotatedCommand(), "fixture-private-command");
    }
}
