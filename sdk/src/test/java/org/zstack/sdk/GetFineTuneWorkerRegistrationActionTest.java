package org.zstack.sdk;

import org.junit.Test;
import static org.junit.Assert.*;

public class GetFineTuneWorkerRegistrationActionTest {
    @Test
    public void queriesGroupRegistrationThroughAuthenticatedReadOnlyRoute() throws Exception {
        GetFineTuneWorkerRegistrationAction action = new GetFineTuneWorkerRegistrationAction();
        RestInfo route = action.getRestInfo();
        assertEquals("GET", route.httpMethod);
        assertEquals("/ai/fine-tune/groups/{uuid}/worker-registration", route.path);
        assertTrue(route.needSession);
        assertFalse(route.needPoll);
        assertTrue(GetFineTuneWorkerRegistrationAction.class.getField("uuid").getAnnotation(Param.class).required());
        action.sessionId = "test-session";
        try { action.checkParameters(); fail("group UUID is required"); }
        catch (ApiException expected) { assertTrue(expected.getMessage().contains("uuid")); }
        action.uuid = "group1";
        action.checkParameters();
    }

    @Test
    public void decodesWorkerProgressWithSourceSchemaAndDistinguishesUnreachable() {
        ApiResult response = new ApiResult();
        response.setResultString("{\"inventory\":{\"reachable\":true,\"registered\":false,\"nnodes\":2,\"registeredRanks\":[],\"missingRanks\":[1],\"staleRanks\":[1]},"
                + "\"schema\":{\"inventory\":\"org.zstack.ai.entity.FineTuneWorkerRegistrationInventory\"}}");
        FineTuneWorkerRegistrationInventory inventory = response.getResult(GetFineTuneWorkerRegistrationResult.class).inventory;
        assertTrue(inventory.reachable); assertFalse(inventory.registered); assertEquals(2, inventory.nnodes);
        assertEquals(1, ((Number) inventory.missingRanks.get(0)).intValue());
        assertEquals(FineTuneWorkerRegistrationInventory.class.getName(), SourceClassMap.srcToDstMapping.get("org.zstack.ai.entity.FineTuneWorkerRegistrationInventory"));
        response.setResultString("{\"inventory\":{\"reachable\":false,\"registered\":false,\"nnodes\":2}}");
        assertFalse(response.getResult(GetFineTuneWorkerRegistrationResult.class).inventory.reachable);
    }
}
