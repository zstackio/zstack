package org.zstack.sdk;

import org.junit.Test;

import static org.junit.Assert.*;

public class GetFineTuneRunLogActionTest {
    @Test
    public void routeAndRequiredFieldsMatchPublicApi() throws Exception {
        GetFineTuneRunLogAction action = new GetFineTuneRunLogAction();
        RestInfo rest = action.getRestInfo();

        assertEquals("GET", rest.httpMethod);
        assertEquals("/ai/fine-tune/runs/{uuid}/logs/{stream}", rest.path);
        assertTrue(rest.needSession);
        assertFalse(rest.needPoll);
        assertTrue(GetFineTuneRunLogAction.class.getField("uuid").getAnnotation(Param.class).required());
        assertTrue(GetFineTuneRunLogAction.class.getField("stream").getAnnotation(Param.class).required());
    }

    @Test
    public void rejectsMissingStreamBeforeSendingRequest() {
        GetFineTuneRunLogAction action = new GetFineTuneRunLogAction();
        action.uuid = "2d4fdf6f4a65482aa8f0e8f2ebd7f9f0";
        action.sessionId = "test-session";

        try {
            action.checkParameters();
            fail("stream is a required path parameter");
        } catch (ApiException expected) {
            assertTrue(expected.getMessage().contains("stream"));
        }

        action.stream = "rank-0";
        action.checkParameters();
    }

    @Test
    public void decodesLogInventoryWithTruncationAndSourceSchema() {
        ApiResult response = new ApiResult();
        response.setResultString("{\"inventory\":{\"status\":\"running\",\"content\":\"step 1\",\"truncated\":true},"
                + "\"schema\":{\"inventory\":\"org.zstack.ai.BentoBasedModelStorageBackend$FineTuneRunLog\"}}");

        GetFineTuneRunLogResult result = response.getResult(GetFineTuneRunLogResult.class);
        assertNotNull(result.inventory);
        assertEquals("running", result.inventory.status);
        assertEquals("step 1", result.inventory.content);
        assertTrue(result.inventory.truncated);
        assertEquals(FineTuneRunLog.class.getName(), SourceClassMap.srcToDstMapping.get(
                "org.zstack.ai.BentoBasedModelStorageBackend$FineTuneRunLog"));
    }
}
