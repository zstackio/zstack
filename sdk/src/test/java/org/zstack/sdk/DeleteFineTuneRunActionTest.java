package org.zstack.sdk;

import org.junit.Test;

import static org.junit.Assert.*;

public class DeleteFineTuneRunActionTest {
    @Test
    public void deletesOneRunByUuidAndWaitsForApiEvent() throws Exception {
        DeleteFineTuneRunAction action = new DeleteFineTuneRunAction();
        RestInfo rest = action.getRestInfo();

        assertEquals("DELETE", rest.httpMethod);
        assertEquals("/ai/fine-tune/runs/{uuid}", rest.path);
        assertTrue(rest.needSession);
        assertTrue(rest.needPoll);
        assertTrue(DeleteFineTuneRunAction.class.getField("uuid").getAnnotation(Param.class).required());
    }

    @Test
    public void rejectsMissingRunUuidBeforeSendingRequest() {
        DeleteFineTuneRunAction action = new DeleteFineTuneRunAction();
        action.sessionId = "test-session";

        try {
            action.checkParameters();
            fail("run UUID is required to avoid deleting an unintended record");
        } catch (ApiException expected) {
            assertTrue(expected.getMessage().contains("uuid"));
        }

        action.uuid = "2d4fdf6f4a65482aa8f0e8f2ebd7f9f0";
        action.checkParameters();
    }
}
