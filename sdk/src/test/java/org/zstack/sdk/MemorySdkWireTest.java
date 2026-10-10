package org.zstack.sdk;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/** Exercises generated SDK and the real HTTP client, not a running management node. */
public class MemorySdkWireTest {
    private HttpServer server;
    private ZSConfig previous;
    private volatile String path, method, authorization, body, query;
    private volatile String response;
    private volatile int status = 200;

    @Before public void start() throws Exception {
        previous = ZSClient.getConfig();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            path = exchange.getRequestURI().getPath(); query = exchange.getRequestURI().getQuery(); method = exchange.getRequestMethod();
            authorization = exchange.getRequestHeaders().getFirst("Authorization");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (InputStream input = exchange.getRequestBody()) {
                byte[] buffer = new byte[4096]; int count;
                while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
            }
            body = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
            byte[] answer = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, answer.length);
            exchange.getResponseBody().write(answer); exchange.close();
        });
        server.start();
        ZSClient.configure(new ZSConfig.Builder().setHostname("127.0.0.1")
                .setPort(server.getAddress().getPort()).setReadTimeout(5, TimeUnit.SECONDS)
                .setDefaultPollingTimeout(5, TimeUnit.SECONDS).build());
    }

    @After public void stop() {
        if (server != null) server.stop(0);
        if (previous != null) ZSClient.configure(previous);
    }

    @Test public void updatePreservesFrozenTargetsAndStringPolicy() {
        response = "{\"inventory\":{\"uuid\":\"task1\",\"status\":\"Unknown\",\"desiredRevision\":7}}";
        UpdateMemoryPolicyAction action = new UpdateMemoryPolicyAction();
        action.sessionId = "local-test-session"; action.scope = "Cluster";
        action.resourceUuid = "global"; action.action = "apply"; action.expectedRevision = 6;
        action.clientRequestUuid = "00000000000000000000000000000001";
        action.expectedControlOperationUuid = "pause-operation-1";
        action.targetHostUuids = Arrays.asList("host1", "host2");
        action.policy = "{\"ksm\":{\"enabled\":true,\"zeroPagesEnabled\":true}}";
        action.expectedSourceRevisions = new java.util.HashMap(); action.expectedSourceRevisions.put("Global:global", 4L);
        action.clearOverrideFields = Arrays.asList("ksm.pagesToScan");
        action.targetSnapshotGeneration = "snap-1"; action.targetShardIndex = 1; action.targetShardCount = 3;
        action.targetTotalCount = 2048L; action.targetDigest = "digest"; action.targetVmUuids = Arrays.asList("vm1", "vm2");
        UpdateMemoryPolicyAction.Result result = action.call();
        assertNull(result.error); assertEquals("Unknown", result.value.inventory.getStatus());
        assertEquals("PUT", method); assertTrue(path.endsWith("/memory-policies/global/actions"));
        assertNotNull(authorization);
        JsonObject request = new JsonParser().parse(body).getAsJsonObject().getAsJsonObject("updateMemoryPolicy");
        assertEquals(action.policy, request.get("policy").getAsString());
        assertEquals(2, request.getAsJsonArray("targetHostUuids").size());
        assertEquals(6, request.get("expectedRevision").getAsLong());
        assertEquals("pause-operation-1", request.get("expectedControlOperationUuid").getAsString());
        assertEquals("Cluster", request.get("scope").getAsString());
        assertEquals("snap-1", request.get("targetSnapshotGeneration").getAsString());
        assertEquals(3, request.get("targetShardCount").getAsInt());
        assertEquals("digest", request.get("targetDigest").getAsString());
        assertTrue(request.has("expectedSourceRevisions"));
        assertEquals(1, request.getAsJsonArray("clearOverrideFields").size());
        assertFalse(request.has("sessionId"));
    }

    @Test public void migrationExclusionSurvivesRealSdkHttpResponse() {
        response = "{\"inventory\":{\"scope\":\"VM\",\"resourceUuid\":\"vm1\","
                + "\"migrationExclusion\":{\"retained\":true,\"sourceHostUuid\":\"host1\","
                + "\"targetHostUuid\":\"host2\",\"sourceRevisions\":\"{\\\"Host:host1\\\":7}\","
                + "\"sourceInstanceGeneration\":\"generation-1\"}}}";
        GetMemoryPolicyAction action = new GetMemoryPolicyAction();
        action.sessionId = "local-test-session"; action.scope = "VM"; action.resourceUuid = "vm1";
        GetMemoryPolicyAction.Result result = action.call();
        assertNull(result.error);
        JsonObject inventory = new com.google.gson.Gson().toJsonTree(result.value.inventory).getAsJsonObject();
        assertTrue("SDK dropped migrationExclusion", inventory.has("migrationExclusion"));
        JsonObject exclusion = inventory.getAsJsonObject("migrationExclusion");
        assertTrue(exclusion.get("retained").getAsBoolean());
        assertEquals("host1", exclusion.get("sourceHostUuid").getAsString());
        assertEquals("host2", exclusion.get("targetHostUuid").getAsString());
        assertEquals("{\"Host:host1\":7}", exclusion.get("sourceRevisions").getAsString());
        assertEquals("generation-1", exclusion.get("sourceInstanceGeneration").getAsString());
    }

    @Test public void taskControlAndReconcileReferencesSurviveRealSdkHttpResponse() {
        response = "{\"inventories\":[{\"uuid\":\"task1\",\"status\":\"Unknown\","
                + "\"expectedControlOperationUuid\":\"control1\",\"reconcileOperationUuid\":\"reconcile1\"}],"
                + "\"total\":1,\"schema\":{\"inventories[0]\":\"org.zstack.kvm.memory.MemoryTaskInventory\"}}";
        QueryMemoryTaskAction action = new QueryMemoryTaskAction(); action.sessionId = "local-test-session";
        QueryMemoryTaskAction.Result result = action.call();
        assertNull(result.error);
        Object task = result.value.inventories.get(0);
        assertTrue(task instanceof MemoryTaskInventory);
        JsonObject inventory = new com.google.gson.Gson().toJsonTree(task).getAsJsonObject();
        assertTrue("SDK dropped expectedControlOperationUuid", inventory.has("expectedControlOperationUuid"));
        assertTrue("SDK dropped reconcileOperationUuid", inventory.has("reconcileOperationUuid"));
        assertEquals("control1", inventory.get("expectedControlOperationUuid").getAsString());
        assertEquals("reconcile1", inventory.get("reconcileOperationUuid").getAsString());
        assertEquals("Unknown", inventory.get("status").getAsString());
    }

    @Test public void recoveryAndLargePolicyUseBackendContractWithoutSdkCharacterCap() throws Exception {
        response = "{\"inventory\":{\"uuid\":\"task1\",\"status\":\"Unknown\"}}";
        UpdateMemoryPolicyAction action = new UpdateMemoryPolicyAction();
        action.sessionId = "local-test-session"; action.resourceUuid = "host1";
        action.action = "recoverUncertain"; action.clientRequestUuid = "new-recovery-request";
        action.expectedControlOperationUuid = "control1";
        action.recovery = new MemoryUncertainRecovery();
        action.recovery.expectedHostBootId = "boot-1";
        action.recovery.expectedPoolGeneration = "pool-7";
        action.recovery.drainControlOperationUuid = "drain-2";
        action.recovery.confirmed = true;
        char[] content = new char[20000]; Arrays.fill(content, 'x');
        action.policy = "{\"testPayload\":\"" + new String(content) + "\"}";
        UpdateMemoryPolicyAction.Result result = action.call();
        assertNull(result.error);
        JsonObject request = new JsonParser().parse(body).getAsJsonObject().getAsJsonObject("updateMemoryPolicy");
        assertEquals("recoverUncertain", request.get("action").getAsString());
        JsonObject recovery = request.getAsJsonObject("recovery");
        assertEquals(4, recovery.entrySet().size());
        assertEquals("boot-1", recovery.get("expectedHostBootId").getAsString());
        assertEquals("pool-7", recovery.get("expectedPoolGeneration").getAsString());
        assertEquals("drain-2", recovery.get("drainControlOperationUuid").getAsString());
        assertTrue(recovery.get("confirmed").getAsBoolean());
        assertFalse("scope is optional for this action", request.has("scope"));
        assertEquals("control1", request.get("expectedControlOperationUuid").getAsString());
        assertEquals(action.policy, request.get("policy").getAsString());

        long exact = 9007199254740993L;
        action.action = "prepareWritebackBackend";
        action.backendPreparation = new MemoryBackendPreparation();
        action.backendPreparation.candidateId = "candidate-1";
        action.backendPreparation.expectedHostBootId = "boot-1";
        action.backendPreparation.expectedPoolGeneration = "pool-7";
        action.backendPreparation.backendCapacityBytes = exact;
        action.backendPreparation.resetConfirmed = true;
        action.recovery = null;
        assertNull(action.call().error);
        request = new JsonParser().parse(body).getAsJsonObject().getAsJsonObject("updateMemoryPolicy");
        JsonObject backend = request.getAsJsonObject("backendPreparation");
        assertEquals(exact, backend.get("backendCapacityBytes").getAsLong());
        assertTrue(backend.get("resetConfirmed").getAsBoolean());
        assertEquals("candidate-1", backend.get("candidateId").getAsString());

        action.action = "prepareZramPool";
        action.poolPreparation = new MemoryZramPoolPreparation();
        action.poolPreparation.expectedHostBootId = "boot-1";
        action.poolPreparation.expectedPoolGeneration = "pool-7";
        action.poolPreparation.resetConfirmed = true;
        action.backendPreparation = null;
        assertNull(action.call().error);
        request = new JsonParser().parse(body).getAsJsonObject().getAsJsonObject("updateMemoryPolicy");
        JsonObject pool = request.getAsJsonObject("poolPreparation");
        assertEquals("pool-7", pool.get("expectedPoolGeneration").getAsString());
        assertTrue(pool.get("resetConfirmed").getAsBoolean());
    }

    @Test public void policyFieldModesAndCamelCasePolicyRoundTrip() {
        long logicalCapacity = 9007199254740993L;
        String policy = "{\"schemaVersion\":1,\"zram\":{\"logicalCapacityBytes\":"
                + logicalCapacity + ",\"ramLimitBytes\":4294967296},"
                + "\"ksm\":{\"pagesToScan\":1250}}";
        response = "{\"inventory\":{\"scope\":\"Host\",\"resourceUuid\":\"host1\","
                + "\"fieldModes\":{\"ksm.enabled\":\"Unmanaged\"}}}";
        GetMemoryPolicyAction get = new GetMemoryPolicyAction();
        get.sessionId = "local-test-session"; get.scope = "Host"; get.resourceUuid = "host1";
        GetMemoryPolicyAction.Result getResult = get.call();
        assertNull(getResult.error);
        assertEquals("Unmanaged", getResult.value.inventory.getFieldModes().get("ksm.enabled"));
        assertEquals(1, getResult.value.inventory.getFieldModes().size());

        response = "{\"inventory\":{\"uuid\":\"task1\",\"status\":\"Unknown\"}}";
        UpdateMemoryPolicyAction update = new UpdateMemoryPolicyAction();
        update.sessionId = "local-test-session"; update.scope = "Host"; update.resourceUuid = "host1";
        update.action = "apply"; update.clientRequestUuid = "request-1"; update.expectedRevision = 4;
        update.policy = policy;
        assertNull(update.call().error);
        JsonObject request = new JsonParser().parse(body).getAsJsonObject().getAsJsonObject("updateMemoryPolicy");
        JsonObject decodedPolicy = new JsonParser().parse(request.get("policy").getAsString()).getAsJsonObject();
        assertEquals(logicalCapacity, decodedPolicy.getAsJsonObject("zram").get("logicalCapacityBytes").getAsLong());
        assertEquals(4294967296L, decodedPolicy.getAsJsonObject("zram").get("ramLimitBytes").getAsLong());
        assertEquals(1250, decodedPolicy.getAsJsonObject("ksm").get("pagesToScan").getAsInt());
        assertFalse(decodedPolicy.getAsJsonObject("ksm").has("enabled"));
    }

    @Test public void nestedInventoryModelsHaveBidirectionalSourceMappings() throws Exception {
        for (String name : Arrays.asList("MemoryVmExclusionInventory", "MemoryHostPolicyPreviewInventory",
                "MemoryBlockedPolicyFieldInventory")) {
            String backend = "org.zstack.kvm.memory." + name;
            String sdk = "org.zstack.sdk." + name;
            assertNotNull(Class.forName(sdk));
            assertEquals(sdk, SourceClassMap.srcToDstMapping.get(backend));
            assertEquals(backend, SourceClassMap.dstToSrcMapping.get(sdk));
        }
    }

    @Test public void previewReadsTargetsWarningsAndEffectivePolicy() {
        response = "{\"inventory\":{\"revision\":2,\"effectivePolicy\":\"{}\"},"
                + "\"hostUuids\":[\"host1\"],\"warnings\":[\"CAPACITY_DEFAULT_UNKNOWN\"],"
                + "\"hostResults\":[{\"hostUuid\":\"host1\",\"eligible\":false,"
                + "\"blockedFields\":[{\"field\":\"zram.enabled\",\"reasonCode\":\"CAPABILITY_STALE\"}]}],"
                + "\"schema\":{\"hostResults[0]\":\"org.zstack.kvm.memory.MemoryHostPolicyPreviewInventory\","
                + "\"hostResults[0].blockedFields[0]\":\"org.zstack.kvm.memory.MemoryBlockedPolicyFieldInventory\"}}";
        PreviewMemoryPolicyAction action = new PreviewMemoryPolicyAction();
        action.sessionId = "local-test-session"; action.scope = "Global";
        action.resourceUuid = "global"; action.policy = "{}";
        action.action = "clearOverride";
        action.clearOverrideFields = Arrays.asList("ksm.pagesToScan");
        action.targetHostUuids = Arrays.asList("host1");
        PreviewMemoryPolicyAction.Result result = action.call();
        assertNull(result.error); assertEquals(2, result.value.inventory.getRevision());
        assertEquals(Arrays.asList("host1"), result.value.hostUuids);
        assertEquals(Arrays.asList("CAPACITY_DEFAULT_UNKNOWN"), result.value.warnings);
        Object hostResult = result.value.hostResults.get(0);
        assertTrue(hostResult instanceof MemoryHostPolicyPreviewInventory);
        MemoryHostPolicyPreviewInventory typedHost = (MemoryHostPolicyPreviewInventory) hostResult;
        assertEquals("host1", typedHost.getHostUuid());
        assertFalse(typedHost.getEligible());
        Object blocked = typedHost.getBlockedFields().get(0);
        assertTrue(blocked instanceof MemoryBlockedPolicyFieldInventory);
        MemoryBlockedPolicyFieldInventory blockedField = (MemoryBlockedPolicyFieldInventory) blocked;
        assertEquals("zram.enabled", blockedField.getField());
        assertEquals("CAPABILITY_STALE", blockedField.getReasonCode());
        assertEquals("POST", method); assertTrue(path.endsWith("/memory-policies/global/preview"));
        JsonObject previewRequest = new JsonParser().parse(body).getAsJsonObject().getAsJsonObject("params");
        assertEquals("clearOverride", previewRequest.get("action").getAsString());
        assertEquals(1, previewRequest.getAsJsonArray("clearOverrideFields").size());
    }

    @Test public void stateKeepsStringSchemaAndUnknownRevision() {
        response = "{\"inventories\":[{\"hostUuid\":\"host1\",\"schemaVersion\":\"memory-state-v1\","
                + "\"status\":\"Unknown\",\"appliedRevision\":null,\"quality\":\"Partial\",\"metrics\":{\"total\":-16}}],\"total\":1,\"nextPage\":2,\"snapshotId\":\"state-snapshot\","
                + "\"schema\":{\"inventories[0]\":\"org.zstack.kvm.memory.MemoryStateInventory\"}}";
        QueryMemoryStateAction action = new QueryMemoryStateAction(); action.sessionId = "local-test-session"; action.snapshotId = "state-snapshot";
        QueryMemoryStateAction.Result result = action.call();
        assertNull(result.error);
        MemoryStateInventory state = (MemoryStateInventory) result.value.inventories.get(0);
        assertEquals("memory-state-v1", state.getSchemaVersion());
        assertNull(state.getAppliedRevision()); assertEquals("Partial", state.getQuality());
        assertEquals(-16, ((Number) state.getMetrics().get("total")).intValue());
        assertEquals(Integer.valueOf(2), result.value.getNextPage());
        assertEquals("state-snapshot", result.value.getSnapshotId());
        assertEquals("GET", method);
    }

    @Test public void taskPaginationAndReadonlyRoutesAreGenerated() {
        response = "{\"inventories\":[],\"total\":2048,\"nextPage\":7,\"snapshotId\":\"task-snapshot\"}";
        QueryMemoryTaskAction task = new QueryMemoryTaskAction(); task.sessionId = "local-test-session"; task.snapshotId = "task-snapshot";
        QueryMemoryTaskAction.Result taskResult = task.call();
        assertNull(taskResult.error); assertEquals(Integer.valueOf(7), taskResult.value.getNextPage());
        assertEquals("task-snapshot", taskResult.value.getSnapshotId());
        assertTrue(path.endsWith("/memory-tasks"));

        response = "{\"inventory\":{\"hostUuid\":\"host1\",\"entries\":[{\"operationId\":\"op1\","
                + "\"vmUuid\":\"vm1\",\"kind\":\"reclaim\",\"status\":\"Unknown\","
                + "\"startedAt\":\"2026-10-10T10:00:00Z\",\"timeoutAt\":\"2026-10-10T10:01:00Z\","
                + "\"completedAt\":null,\"reason\":\"WAITING\"}],\"total\":1,\"nextPage\":2,"
                + "\"concurrency\":{\"activeOperations\":1,\"isolatedTimeoutOperations\":0},"
                + "\"recordBudget\":{\"actualAllocatedBytes\":1024,\"reservedBytes\":256,"
                + "\"budgetBytes\":1073741824,\"records\":1,\"protectedRecords\":0}},"
                + "\"schema\":{\"inventory.entries[0]\":\"org.zstack.kvm.memory.MemoryHostOperationsInventory$Entry\"}}";
        QueryHostMemoryOperationsAction operations = new QueryHostMemoryOperationsAction();
        operations.sessionId = "local-test-session"; operations.hostUuid = "host1";
        QueryHostMemoryOperationsAction.Result operationsResult = operations.call();
        assertNull(operationsResult.error);
        MemoryHostOperationsInventory operationsInventory = operationsResult.value.getInventory();
        assertEquals("host1", operationsInventory.getHostUuid());
        assertEquals(Long.valueOf(1), operationsInventory.getTotal());
        assertEquals(Integer.valueOf(2), operationsInventory.getNextPage());
        Object operation = operationsInventory.getEntries().get(0);
        assertTrue(operation instanceof Entry);
        assertEquals("op1", ((Entry) operation).getOperationId());
        assertEquals("vm1", ((Entry) operation).getVmUuid());
        assertEquals("Unknown", ((Entry) operation).getStatus());
        assertEquals(Integer.valueOf(1), operationsInventory.getConcurrency().getActiveOperations());
        assertEquals(Long.valueOf(1073741824L), operationsInventory.getRecordBudget().getBudgetBytes());
        assertTrue(path.endsWith("/hosts/host1/memory-operations"));

        response = "{\"inventory\":{\"vmUuid\":\"vm1\",\"hostUuid\":\"host1\",\"quality\":\"complete\","
                + "\"metrics\":{\"ramOriginalBytes\":9007199254740993,\"ramPayloadBytes\":4294967296}}}";
        GetVmMemoryOptimizationAction vm = new GetVmMemoryOptimizationAction();
        vm.sessionId = "local-test-session"; vm.vmUuid = "vm1";
        GetVmMemoryOptimizationAction.Result vmResult = vm.call();
        assertNull(vmResult.error); assertEquals("vm1", vmResult.value.getInventory().getVmUuid());
        assertEquals("host1", vmResult.value.getInventory().getHostUuid());
        assertEquals(9007199254740993L, vmResult.value.getInventory().getMetrics().getRamOriginalBytes().longValue());
        assertEquals(4294967296L, vmResult.value.getInventory().getMetrics().getRamPayloadBytes().longValue());
        assertTrue(path.endsWith("/vm-instances/vm1/memory-optimization"));
    }

    @Test public void writebackBackendInventoryUsesGeneratedTypedModels() {
        response = "{\"inventory\":{\"hostUuid\":\"host1\",\"bootId\":\"boot-1\","
                + "\"status\":\"Available\",\"candidatesFieldPresent\":true,\"candidates\":[{"
                + "\"identity\":\"dev:8:1\",\"fingerprint\":\"fingerprint-1\",\"path\":\"/dev/vdb\","
                + "\"capacityBytes\":9007199254740993,\"eligible\":true,\"stableIdentity\":{"
                + "\"device\":\"/dev/vdb\",\"majorMinor\":\"8:16\",\"capacityBytes\":9007199254740993}}]},"
                + "\"schema\":{\"inventory.candidates[0]\":\"org.zstack.kvm.memory.MemoryWritebackBackendCandidateInventory\"}}";
        GetHostMemoryWritebackBackendsAction action = new GetHostMemoryWritebackBackendsAction();
        action.sessionId = "local-test-session"; action.hostUuid = "host1";
        GetHostMemoryWritebackBackendsAction.Result result = action.call();
        assertNull(result.error);
        MemoryWritebackBackendInventory inventory = result.value.getInventory();
        assertEquals("host1", inventory.getHostUuid());
        assertEquals(Boolean.TRUE, inventory.getCandidatesFieldPresent());
        Object value = inventory.getCandidates().get(0);
        assertTrue(value instanceof MemoryWritebackBackendCandidateInventory);
        MemoryWritebackBackendCandidateInventory candidate = (MemoryWritebackBackendCandidateInventory) value;
        assertEquals("fingerprint-1", candidate.getFingerprint());
        assertEquals(Long.valueOf(9007199254740993L), candidate.getCapacityBytes());
        assertTrue(candidate.getEligible());
        assertNotNull(candidate.getStableIdentity());
        assertEquals("8:16", candidate.getStableIdentity().getMajorMinor());
        assertEquals(Long.valueOf(9007199254740993L), candidate.getStableIdentity().getCapacityBytes());
        assertEquals("GET", method);
        assertTrue(path.endsWith("/hosts/host1/memory-writeback-backends"));
    }

    private JsonObject callGenerated(String name, java.util.Map<String, Object> params) throws Exception {
        Class<?> type = Class.forName("org.zstack.sdk." + name + "Action");
        Object action = type.newInstance();
        type.getField("sessionId").set(action, "local-test-session");
        for (java.util.Map.Entry<String, Object> entry : params.entrySet()) {
            type.getField(entry.getKey()).set(action, entry.getValue());
        }
        Object result = type.getMethod("call").invoke(action);
        assertNull(result.getClass().getField("error").get(result));
        Object value = result.getClass().getField("value").get(result);
        assertNotNull(value);
        String resultName = name.equals("GetMemoryStates") ? "QueryMemoryState"
                : name.equals("GetMemoryTasks") ? "QueryMemoryTask"
                : name.equals("GetHostMemoryOperations") ? "QueryHostMemoryOperations" : name;
        assertEquals("org.zstack.sdk." + resultName + "Result", value.getClass().getName());
        if (value instanceof QueryHostMemoryOperationsResult) {
            Object entry = ((QueryHostMemoryOperationsResult) value).inventory.getEntries().get(0);
            assertTrue(entry instanceof Entry);
            assertEquals("operation1", ((Entry) entry).getOperationId());
            assertEquals("Unknown", ((Entry) entry).getStatus());
        }
        assertEquals("GET", method);
        return new com.google.gson.Gson().toJsonTree(value).getAsJsonObject();
    }

    @Test public void getStateAliasAndLegacyRoutePreserveInheritedFiltersAndSnapshot() throws Exception {
        for (String name : Arrays.asList("GetMemoryStates", "QueryMemoryState")) {
            response = "{\"inventories\":[],\"total\":12,\"nextPage\":4,\"snapshotId\":\"state-token\"}";
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("hostUuids", Arrays.asList("host1", "host2"));
            params.put("start", 2); params.put("limit", 2); params.put("snapshotId", "state-token");
            JsonObject result = callGenerated(name, params);
            assertTrue(path.endsWith(name.startsWith("Get") ? "/memory-optimization/states" : "/memory-states"));
            assertTrue(query.contains("hostUuids=host1")); assertTrue(query.contains("hostUuids=host2"));
            assertTrue(query.contains("start=2")); assertTrue(query.contains("limit=2"));
            assertTrue(query.contains("snapshotId=state-token"));
            assertEquals("state-token", result.get("snapshotId").getAsString());
            assertEquals(12, result.get("total").getAsInt());
            assertEquals(4, result.get("nextPage").getAsInt());
        }
    }

    @Test public void statePaginationNegativeStartIsRejectedBeforeHttpOnBothRoutes() throws Exception {
        response = "{\"inventories\":[],\"total\":0}";
        for (String name : Arrays.asList("GetMemoryStates", "QueryMemoryState")) {
            Class<?> type = Class.forName("org.zstack.sdk." + name + "Action");
            Object action = type.newInstance();
            type.getField("sessionId").set(action, "local-test-session");
            type.getField("start").setInt(action, -1);
            try {
                type.getMethod("call").invoke(action);
                fail("negative start must be rejected: " + name);
            } catch (java.lang.reflect.InvocationTargetException expected) {
                assertTrue(expected.getCause() instanceof ApiException);
                assertTrue(expected.getCause().getMessage().contains("start"));
            }
            assertNull("invalid pagination must not send an HTTP request", path);
        }
    }

    @Test public void statePaginationAcceptsZeroAndIntegerMaximumOnBothRoutes() throws Exception {
        response = "{\"inventories\":[],\"total\":0}";
        for (String name : Arrays.asList("GetMemoryStates", "QueryMemoryState")) {
            for (int start : new int[] {0, Integer.MAX_VALUE}) {
                java.util.Map<String, Object> params = new java.util.HashMap<>();
                params.put("start", start);
                callGenerated(name, params);
                assertTrue(path.endsWith(name.startsWith("Get")
                        ? "/memory-optimization/states" : "/memory-states"));
                assertTrue(query.contains("start=" + start));
            }
        }
    }

    @Test public void getTaskAliasAndLegacyRoutePreserveRecoveryFieldsAndFilters() throws Exception {
        for (String name : Arrays.asList("GetMemoryTasks", "QueryMemoryTask")) {
            response = "{\"inventories\":[{\"uuid\":\"task1\",\"status\":\"Unknown\","
                    + "\"expectedControlOperationUuid\":\"control1\",\"reconcileOperationUuid\":\"recover1\"}],"
                    + "\"total\":1,\"snapshotId\":\"task-token\","
                    + "\"schema\":{\"inventories[0]\":\"org.zstack.kvm.memory.MemoryTaskInventory\"}}";
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("uuid", "task1"); params.put("hostUuid", "host1"); params.put("status", "Unknown");
            params.put("start", 0); params.put("limit", 1); params.put("snapshotId", "task-token");
            JsonObject result = callGenerated(name, params);
            assertTrue(path.endsWith(name.startsWith("Get") ? "/memory-optimization/tasks" : "/memory-tasks"));
            assertTrue(query.contains("uuid=task1")); assertTrue(query.contains("hostUuid=host1"));
            assertTrue(query.contains("status=Unknown")); assertTrue(query.contains("snapshotId=task-token"));
            JsonObject task = result.getAsJsonArray("inventories").get(0).getAsJsonObject();
            assertEquals("control1", task.get("expectedControlOperationUuid").getAsString());
            assertEquals("recover1", task.get("reconcileOperationUuid").getAsString());
        }
    }

    @Test public void getHostOperationsAliasAndLegacyRoutePreserveInheritedParameters() throws Exception {
        for (String name : Arrays.asList("GetHostMemoryOperations", "QueryHostMemoryOperations")) {
            response = "{\"inventory\":{\"hostUuid\":\"host1\",\"total\":1,\"entries\":[{"
                    + "\"status\":\"Unknown\",\"operationId\":\"operation1\"}]},\"schema\":{"
                    + "\"inventory.entries[0]\":\"org.zstack.kvm.memory.MemoryHostOperationsInventory$Entry\"}}";
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("hostUuid", "host1"); params.put("operationId", "operation1");
            params.put("vmUuid", "vm1"); params.put("status", "Unknown"); params.put("start", 2); params.put("limit", 3);
            JsonObject result = callGenerated(name, params);
            assertTrue(path.endsWith(name.startsWith("Get") ? "/hosts/host1/memory-optimization/operations"
                    : "/hosts/host1/memory-operations"));
            assertTrue(query.contains("operationId=operation1")); assertTrue(query.contains("vmUuid=vm1"));
            assertTrue(query.contains("status=Unknown")); assertTrue(query.contains("start=2"));
            assertTrue(query.contains("limit=3")); assertFalse(query.contains("hostUuid="));
            JsonObject inventory = result.getAsJsonObject("inventory");
            assertEquals("host1", inventory.get("hostUuid").getAsString());
            assertEquals(1, inventory.get("total").getAsInt());
            JsonObject entry = inventory.getAsJsonArray("entries").get(0).getAsJsonObject();
            assertEquals("Unknown", entry.get("status").getAsString());
            assertEquals("operation1", entry.get("operationId").getAsString());
        }
    }

    @Test public void previewPreservesStructuredWarningsAndLegacyStrings() throws Exception {
        response = "{\"warnings\":[\"需要再次预检\"],\"warningDetails\":[{"
                + "\"code\":\"ZRAM_CAPACITY_UNVERIFIED\",\"message\":\"需要再次预检\","
                + "\"messageKey\":\"memory.preview.warning.zramCapacityUnverified\",\"formatArgs\":[]}],\"hostUuids\":[]}";
        PreviewMemoryPolicyAction action = new PreviewMemoryPolicyAction();
        action.sessionId = "local-test-session"; action.scope = "Host"; action.resourceUuid = "host1"; action.policy = "{}";
        PreviewMemoryPolicyAction.Result result = action.call();
        assertNull(result.error);
        JsonObject value = new com.google.gson.Gson().toJsonTree(result.value).getAsJsonObject();
        assertTrue("SDK dropped warningDetails", value.has("warningDetails"));
        assertEquals("需要再次预检", value.getAsJsonArray("warnings").get(0).getAsString());
        JsonObject detail = value.getAsJsonArray("warningDetails").get(0).getAsJsonObject();
        assertEquals("ZRAM_CAPACITY_UNVERIFIED", detail.get("code").getAsString());
        assertEquals("memory.preview.warning.zramCapacityUnverified", detail.get("messageKey").getAsString());
        assertEquals(0, detail.getAsJsonArray("formatArgs").size());
        String backend = "org.zstack.kvm.memory.MemoryPreviewWarningInventory";
        assertEquals("org.zstack.sdk.MemoryPreviewWarningInventory", SourceClassMap.srcToDstMapping.get(backend));
    }

    @Test public void revisionConflictDoesNotBecomeSuccess() {
        status = 503; response = "{\"error\":{\"code\":\"MEMORY_REVISION_CONFLICT\",\"description\":\"preview again\"}}";
        CancelMemoryTaskAction action = new CancelMemoryTaskAction();
        action.sessionId = "local-test-session"; action.uuid = "task1";
        CancelMemoryTaskAction.Result result = action.call();
        assertNotNull(result.error);
        // The existing SDK wraps initial non-2xx responses in HTTP_ERROR;
        // retain its behavior and verify the backend error is not lost.
        assertTrue(result.error.details.contains("MEMORY_REVISION_CONFLICT"));
        assertNull(result.value);
    }

    @Test public void deleteMemoryTaskUsesGeneratedDeleteRouteAndParsesAuditResponse() {
        response = "{\"taskUuid\":\"1234567890abcdef1234567890abcdef\",\"deleted\":true,"
                + "\"scope\":\"Host\",\"resourceUuid\":\"host1\",\"hostUuids\":[\"host1\"]}";
        DeleteMemoryTaskAction action = new DeleteMemoryTaskAction();
        action.sessionId = "local-test-session";
        action.uuid = "1234567890abcdef1234567890abcdef";
        DeleteMemoryTaskAction.Result result = action.call();
        assertNull(result.error);
        assertEquals("DELETE", method);
        assertTrue(path.endsWith("/memory-optimization/tasks/1234567890abcdef1234567890abcdef"));
        assertNotNull(authorization);
        assertEquals("1234567890abcdef1234567890abcdef", result.value.getTaskUuid());
        assertTrue(result.value.getDeleted());
        assertEquals("Host", result.value.getScope());
        assertEquals("host1", result.value.getResourceUuid());
        assertEquals(Arrays.asList("host1"), result.value.getHostUuids());
    }

    @Test public void policyAndStateSdkModelsCarryBootstrapAndPlanDiagnostics() {
        response = "{\"inventory\":{\"scope\":\"Global\",\"resourceUuid\":\"global\","
                + "\"bootstrapStatus\":\"PENDING\",\"bootstrapReason\":\"MEMORY_LICENSE_UNAVAILABLE\"}}";
        GetMemoryPolicyAction policy = new GetMemoryPolicyAction();
        policy.sessionId = "local-test-session"; policy.scope = "Global"; policy.resourceUuid = "global";
        GetMemoryPolicyAction.Result policyResult = policy.call();
        assertNull(policyResult.error);
        assertEquals("PENDING", policyResult.value.inventory.getBootstrapStatus());
        assertEquals("MEMORY_LICENSE_UNAVAILABLE", policyResult.value.inventory.getBootstrapReason());

        String stateJson = "{\"policyPlanStatus\":\"NeedsReview\","
                + "\"policyBlockedFields\":\"{\\\"policy\\\":{\\\"reason\\\":\\\"LEGACY_APPLIED_POLICY_UNKNOWN\\\"}}\","
                + "\"appliedPolicy\":\"{\\\"schemaVersion\\\":1,\\\"ksm\\\":{\\\"enabled\\\":false}}\","
                + "\"featureState\":\"Active\",\"featureStateSource\":\"KSM_NATIVE\","
                + "\"appliedPolicyHash\":\"applied-hash\",\"policyTargetHash\":\"target-hash\","
                + "\"policyPlanHash\":\"plan-hash\"}";
        MemoryStateInventory state = new com.google.gson.Gson().fromJson(stateJson, MemoryStateInventory.class);
        assertEquals("NeedsReview", state.getPolicyPlanStatus());
        assertEquals("KSM_NATIVE", state.getFeatureStateSource());
        assertTrue(state.getPolicyBlockedFields().contains("LEGACY_APPLIED_POLICY_UNKNOWN"));
        assertEquals("applied-hash", state.getAppliedPolicyHash());
        assertEquals("target-hash", state.getPolicyTargetHash());
        assertEquals("plan-hash", state.getPolicyPlanHash());

        String taskJson = "{\"policyPlanHash\":\"task-plan-hash\"}";
        MemoryTaskInventory task = new com.google.gson.Gson().fromJson(taskJson, MemoryTaskInventory.class);
        assertEquals("task-plan-hash", task.getPolicyPlanHash());
    }

    @Test public void batchVmAccountingUsesCollectionRouteAndPreservesUnavailableEntries() {
        response = "{\"inventories\":{\"vm1\":{\"vmUuid\":\"vm1\",\"quality\":\"source_memcg_inventory\","
                + "\"metrics\":{\"ramOriginalBytes\":100}},\"vm2\":{\"vmUuid\":\"vm2\",\"quality\":\"unavailable\","
                + "\"reason\":\"HOST_ACCOUNTING_UNAVAILABLE\"}},"
                + "\"schema\":{\"inventories.vm1\":\"org.zstack.kvm.memory.MemoryVmAccountingInventory\","
                + "\"inventories.vm2\":\"org.zstack.kvm.memory.MemoryVmAccountingInventory\"}}";
        GetVmMemoryOptimizationsAction action = new GetVmMemoryOptimizationsAction();
        action.sessionId = "local-test-session"; action.vmUuids = Arrays.asList("vm1", "vm2");
        GetVmMemoryOptimizationsAction.Result result = action.call();
        assertNull(result.error);
        Object vm1 = result.value.inventories.get("vm1");
        Object vm2 = result.value.inventories.get("vm2");
        assertTrue(vm1 instanceof MemoryVmAccountingInventory);
        assertTrue(vm2 instanceof MemoryVmAccountingInventory);
        MemoryVmAccountingInventory available = (MemoryVmAccountingInventory) vm1;
        MemoryVmAccountingInventory unavailable = (MemoryVmAccountingInventory) vm2;
        assertEquals("source_memcg_inventory", available.getQuality());
        assertEquals(Long.valueOf(100), available.getMetrics().getRamOriginalBytes());
        assertEquals("unavailable", unavailable.getQuality());
        assertEquals("HOST_ACCOUNTING_UNAVAILABLE", unavailable.getReason());
        assertEquals("GET", method); assertTrue(path.endsWith("/vm-instances/memory-optimizations"));
        assertNotNull(query); assertTrue(query.contains("vmUuids"));
    }

    @Test public void summaryScopesZoneAndKeepsCurrentStatusSeparateFromSavingsTime() {
        response = "{\"summary\":{\"sampleTime\":1000,\"totalSavedEstimateBytes\":null,"
                + "\"metricCoverage\":{\"zramBytes\":{\"expectedHosts\":2,\"coveredHosts\":1,\"quality\":\"Partial\"},"
                + "\"zramNormalBytes\":{\"expectedHosts\":2,\"coveredHosts\":1,\"quality\":\"Partial\"}},"
                + "\"currentStatus\":{\"stale\":1,\"rebootRequired\":1},"
                + "\"currentStatusSampleTime\":2000,\"currentStatusTtlMillis\":6000}}";
        GetMemorySummaryAction action = new GetMemorySummaryAction();
        action.sessionId = "local-test-session";
        action.zoneUuid = "zone1";
        action.hostUuids = Arrays.asList("host1");
        GetMemorySummaryAction.Result result = action.call();
        assertNull(result.error);
        assertEquals("GET", method); assertTrue(path.endsWith("/memory-summary"));
        assertTrue(query.contains("zoneUuid=zone1")); assertTrue(query.contains("hostUuids"));
        assertNull(result.value.summary.getTotalSavedEstimateBytes());
        assertEquals(Long.valueOf(1000), result.value.summary.getSampleTime());
        assertEquals(Long.valueOf(2000), result.value.summary.getCurrentStatusSampleTime());
        assertEquals(Long.valueOf(6000), result.value.summary.getCurrentStatusTtlMillis());
        assertEquals(1, ((Number) result.value.summary.getCurrentStatus().get("stale")).intValue());
        assertEquals(1, ((Number) result.value.summary.getCurrentStatus().get("rebootRequired")).intValue());
        assertEquals("Partial", ((java.util.Map) result.value.summary.getMetricCoverage().get("zramBytes")).get("quality"));
        assertEquals("Partial", ((java.util.Map) result.value.summary.getMetricCoverage().get("zramNormalBytes")).get("quality"));
    }
}
