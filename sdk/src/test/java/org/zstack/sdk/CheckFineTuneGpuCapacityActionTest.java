package org.zstack.sdk;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import org.junit.Test;

import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class CheckFineTuneGpuCapacityActionTest {
    @Test
    public void capacityDecisionsUseOneSynchronousRequestAndThePublishedReplyEnvelope() throws Exception {
        for (boolean passed : new boolean[]{true, false}) {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            AtomicInteger requests = new AtomicInteger();
            AtomicReference<String> method = new AtomicReference<>();
            AtomicReference<String> path = new AtomicReference<>();
            AtomicReference<JsonObject> body = new AtomicReference<>();
            server.createContext("/zstack/v1/ai/fine-tuning/gpu-capacity/actions", exchange -> {
                requests.incrementAndGet();
                method.set(exchange.getRequestMethod());
                path.set(exchange.getRequestURI().toString());
                body.set(new Gson().fromJson(new InputStreamReader(exchange.getRequestBody(),
                        StandardCharsets.UTF_8), JsonObject.class));
                byte[] response = ("{\"inventory\":{\"passed\":" + passed
                        + ",\"requiredHostCount\":2,\"qualifiedHostCount\":1,\"gpuSpecUuid\":\"spec\","
                        + "\"reason\":\"capacity decision\"},\"schema\":{\"inventory\":"
                        + "\"org.zstack.ai.service.training.DistributedTraining$GpuCapacity\"}}")
                        .getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.start();
            try {
                CheckFineTuneGpuCapacityAction action = action();
                action.hostCount = 2;
                ZSConfig config = new ZSConfig.Builder().setHostname("127.0.0.1")
                        .setPort(server.getAddress().getPort()).setContextPath("zstack")
                        .setReadTimeout(5, TimeUnit.SECONDS).build();
                ApiResult response = ZSClient.callWithConfig(action, config);
                assertNull(response.error);
                GpuCapacity inventory = response.getResult(CheckFineTuneGpuCapacityResult.class).inventory;
                assertEquals(passed, inventory.passed);
                assertEquals(2, inventory.requiredHostCount);
                assertEquals(1, inventory.qualifiedHostCount);
                assertEquals("spec", inventory.gpuSpecUuid);
                assertEquals("capacity decision", inventory.reason);
                assertEquals(1, requests.get());
                assertEquals("PUT", method.get());
                assertEquals("/zstack/v1/ai/fine-tuning/gpu-capacity/actions", path.get());
                assertEquals(1, body.get().entrySet().size());
                JsonObject params = body.get().getAsJsonObject("checkFineTuneGpuCapacity");
                assertEquals("service", params.get("modelServiceUuid").getAsString());
                assertEquals(2, params.get("hostCount").getAsInt());
                assertEquals(1, params.get("gpuCount").getAsInt());
            } finally {
                server.stop(0);
            }
        }
    }

    @Test
    public void positiveIntegerCountsHaveNoInventedNodeCeiling() {
        CheckFineTuneGpuCapacityAction action = action();
        action.hostCount = Integer.MAX_VALUE;
        action.checkParameters();
        action.hostCount = 0;
        try { action.checkParameters(); fail("zero hosts cannot describe a deployment"); }
        catch (ApiException expected) { assertTrue(expected.getMessage().contains("hostCount")); }
        action.hostCount = 1;
        action.gpuCount = 0;
        try { action.checkParameters(); fail("an explicit GPU count must be positive"); }
        catch (ApiException expected) { assertTrue(expected.getMessage().contains("gpuCount")); }
    }

    private CheckFineTuneGpuCapacityAction action() {
        CheckFineTuneGpuCapacityAction action = new CheckFineTuneGpuCapacityAction();
        action.modelServiceUuid = "service";
        action.zoneUuid = "zone";
        action.type = "VirtualMachine";
        action.gpuSpecUuid = "spec";
        action.gpuCount = 1;
        action.hostCount = 1;
        action.sessionId = "test-session";
        return action;
    }
}
