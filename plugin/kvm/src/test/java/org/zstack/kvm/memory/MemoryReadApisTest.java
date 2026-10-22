package org.zstack.kvm.memory;

import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.zstack.header.rest.RestRequest;
import java.util.*;
import static org.junit.Assert.*;

public class MemoryReadApisTest {
    public static class TypedNumberFixture {
        public Long whole;
        public Double fraction;
    }

    @Test public void currentStateHostFilterUsesPlatformResourceValidation() throws Exception {
        org.zstack.header.message.APIParam parameter = APIQueryMemoryStateMsg.class.getDeclaredField("hostUuids")
                .getAnnotation(org.zstack.header.message.APIParam.class);
        assertFalse(parameter.required());
        assertEquals(org.zstack.header.host.HostVO.class, parameter.resourceType());
        assertEquals(APIQueryMemoryStateMsg.class, APIGetMemoryStatesMsg.class.getSuperclass());
        // Retained task history must remain queryable after its Host is deleted.
        org.zstack.header.message.APIParam history = APIQueryMemoryTaskMsg.class.getDeclaredField("hostUuid")
                .getAnnotation(org.zstack.header.message.APIParam.class);
        assertNotEquals(org.zstack.header.host.HostVO.class, history.resourceType());
    }
    @Test public void stateExampleUsesActualStatusAndJsonObservationContract() throws Exception {
        MemoryStateInventory example = MemoryStateInventory.__example__();
        assertEquals("Succeeded", example.getStatus());
        assertTrue(new com.google.gson.JsonParser().parse(example.getState()).isJsonObject());
        String expectedFeature = example.getFeatureState();
        String expectedSource = example.getFeatureStateSource();
        MemoryStatePresentation.populate(example, example.getLastSampleTime() + 2000L);
        assertEquals(expectedFeature, example.getFeatureState());
        assertEquals(expectedSource, example.getFeatureStateSource());
        assertEquals("Unknown", example.getQuality());
        assertTrue(example.getMetrics().isEmpty());
        byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(example.getAppliedPolicy().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte value : digest) { hex.append(String.format("%02x", value & 0xff)); }
        assertEquals(hex.toString(), example.getAppliedPolicyHash());
    }

    @Test public void httpResponseMapPreservesFullUint64() {
        Map<String, Object> httpResponse = org.zstack.utils.gson.JSONObjectUtil.toObject(
                "{\"pool_generation\":18446744073709551615}", LinkedHashMap.class);
        assertEquals(java.math.BigInteger.class, httpResponse.get("pool_generation").getClass());
        assertExactUnsignedInteger("18446744073709551615", httpResponse.get("pool_generation"));
    }

    @Test public void cloudBusReplyPreservesAgentStateNumbers() {
        Map<String, Object> agentState = new LinkedHashMap<>();
        agentState.put("pool_generation", 7473813575907645399L);
        agentState.put("uint64_max", new java.math.BigInteger("18446744073709551615"));
        agentState.put("fraction", 1.25d);
        agentState.put("exponent", 1e3d);
        Map<String, Object> agentResponse = new LinkedHashMap<>();
        agentResponse.put("success", true);
        agentResponse.put("state", agentState);
        org.zstack.kvm.KVMHostAsyncHttpCallReply reply = new org.zstack.kvm.KVMHostAsyncHttpCallReply();
        reply.setResponse((LinkedHashMap) agentResponse);
        org.zstack.kvm.KVMHostAsyncHttpCallReply received = (org.zstack.kvm.KVMHostAsyncHttpCallReply)
                org.zstack.core.cloudbus.CloudBusGson.fromJson(
                        org.zstack.core.cloudbus.CloudBusGson.toJson(reply));
        org.zstack.kvm.memory.MemoryAgentResponse response = received.toResponse(
                org.zstack.kvm.memory.MemoryAgentResponse.class);
        Map<?, ?> returnedState = response.state;
        assertEquals(Long.class, returnedState.get("pool_generation").getClass());
        assertExactUnsignedInteger("7473813575907645399", returnedState.get("pool_generation"));
        assertEquals(java.math.BigInteger.class, returnedState.get("uint64_max").getClass());
        assertExactUnsignedInteger("18446744073709551615", returnedState.get("uint64_max"));
        assertEquals(Double.class, returnedState.get("fraction").getClass());
        assertEquals(Double.class, returnedState.get("exponent").getClass());
        assertEquals(0, new java.math.BigDecimal("1.25").compareTo(
                new java.math.BigDecimal(returnedState.get("fraction").toString())));
        assertEquals(0, new java.math.BigDecimal("1000").compareTo(
                new java.math.BigDecimal(returnedState.get("exponent").toString())));
        assertTrue(org.zstack.utils.gson.JSONObjectUtil.toJsonString(returnedState)
                .contains("\"pool_generation\":7473813575907645399"));

        TypedNumberFixture typed = org.zstack.utils.gson.JSONObjectUtil.toObject(
                "{\"fraction\":1.25}", TypedNumberFixture.class);
        assertEquals(Double.class, typed.fraction.getClass());
    }

    @Test public void httpResponseMapKeepsExistingSignedAndDecimalNumberTypes() {
        Map<String, Object> httpResponse = org.zstack.utils.gson.JSONObjectUtil.toObject(
                "{\"zero\":0,\"one\":1,\"negative\":-1,\"belowSafe\":9007199254740991,"
                        + "\"atSafe\":9007199254740992,\"aboveSafe\":9007199254740993,"
                        + "\"longMax\":9223372036854775807,"
                        + "\"longMin\":-9223372036854775808,\"fraction\":1.25,\"exponent\":1e3}",
                LinkedHashMap.class);
        for (String key : Arrays.asList("zero", "one", "negative", "belowSafe", "atSafe", "aboveSafe", "longMax", "longMin")) {
            assertEquals(key, Long.class, httpResponse.get(key).getClass());
        }
        assertEquals(Long.valueOf(9007199254740991L), httpResponse.get("belowSafe"));
        assertEquals(Long.valueOf(9007199254740992L), httpResponse.get("atSafe"));
        assertEquals(Long.valueOf(9007199254740993L), httpResponse.get("aboveSafe"));
        assertEquals(Long.valueOf(Long.MAX_VALUE), httpResponse.get("longMax"));
        assertEquals(Long.valueOf(Long.MIN_VALUE), httpResponse.get("longMin"));
        assertEquals(Double.class, httpResponse.get("fraction").getClass());
        assertEquals(Double.class, httpResponse.get("exponent").getClass());
        Map<String, Object> uint64 = org.zstack.utils.gson.JSONObjectUtil.toObject(
                "{\"max\":18446744073709551615,\"overflow\":18446744073709551616,"
                        + "\"negativeOverflow\":-9223372036854775809,\"negativeZero\":-0}",
                LinkedHashMap.class);
        assertEquals(java.math.BigInteger.class, uint64.get("max").getClass());
        assertExactUnsignedInteger("18446744073709551615", uint64.get("max"));
        assertEquals(Double.class, uint64.get("overflow").getClass());
        assertEquals(Double.class, uint64.get("negativeOverflow").getClass());
        assertEquals(Long.class, uint64.get("negativeZero").getClass());
        assertEquals(Long.valueOf(0), uint64.get("negativeZero"));
        assertEquals(0, new java.math.BigDecimal("1.25").compareTo(
                new java.math.BigDecimal(httpResponse.get("fraction").toString())));
        assertEquals(0, new java.math.BigDecimal("1000").compareTo(
                new java.math.BigDecimal(httpResponse.get("exponent").toString())));

        TypedNumberFixture typed = org.zstack.utils.gson.JSONObjectUtil.toObject(
                "{\"whole\":9223372036854775807,\"fraction\":1.25}", TypedNumberFixture.class);
        assertEquals(Long.valueOf(Long.MAX_VALUE), typed.whole);
        assertEquals(Double.class, typed.fraction.getClass());
    }

    @Test public void cloudBusReplyKeepsExistingSafeMapNumbersAndTypedFields() {
        String rawMessage = "{\"org.zstack.kvm.KVMHostAsyncHttpCallReply\":{" +
                "\"response\":{\"success\":true,\"appliedRevision\":7,\"sampleTime\":11," +
                "\"state\":{\"zero\":0,\"one\":1,\"negative\":-1," +
                "\"safeMax\":9007199254740991," +
                "\"fraction\":1.25,\"exponent\":1e3}}}}";
        org.zstack.kvm.KVMHostAsyncHttpCallReply reply = (org.zstack.kvm.KVMHostAsyncHttpCallReply)
                org.zstack.core.cloudbus.CloudBusGson.fromJson(rawMessage);
        org.zstack.kvm.memory.MemoryAgentResponse response = reply.toResponse(
                org.zstack.kvm.memory.MemoryAgentResponse.class);
        Map<?, ?> state = response.state;
        for (String key : Arrays.asList("zero", "one", "negative", "safeMax")) {
            assertEquals(key, Double.class, state.get(key).getClass());
        }
        Map<String, Object> expectedSafeValues = new LinkedHashMap<>();
        expectedSafeValues.put("zero", 0.0d); expectedSafeValues.put("one", 1.0d);
        expectedSafeValues.put("negative", -1.0d); expectedSafeValues.put("safeMax", 9007199254740991d);
        Map<String, Object> returnedSafeValues = new LinkedHashMap<>();
        for (String key : expectedSafeValues.keySet()) { returnedSafeValues.put(key, state.get(key)); }
        assertEquals(expectedSafeValues, returnedSafeValues);
        assertEquals(Long.valueOf(7L), response.appliedRevision);
        assertEquals(Long.valueOf(11L), response.sampleTime);
        assertEquals(Double.class, state.get("fraction").getClass());
        assertEquals(Double.class, state.get("exponent").getClass());
        assertEquals(0, new java.math.BigDecimal("1000").compareTo(
                new java.math.BigDecimal(state.get("exponent").toString())));

        String boundaryMessage = "{\"org.zstack.kvm.KVMHostAsyncHttpCallReply\":{" +
                "\"response\":{\"success\":true,\"state\":{" +
                "\"safeMax\":9007199254740991,\"safeMin\":-9007199254740991," +
                "\"twoTo53\":9007199254740992,\"twoTo53PlusTwo\":9007199254740994," +
                "\"aboveSafe\":9007199254740993,\"belowSafe\":-9007199254740992," +
                "\"longMax\":9223372036854775807,\"longMin\":-9223372036854775808," +
                "\"uint64\":9223372036854775808,\"uint64Max\":18446744073709551615," +
                "\"uint64Overflow\":18446744073709551616," +
                "\"negativeOverflow\":-9223372036854775809,\"negativeZero\":-0," +
                "\"fraction\":1.25,\"exponent\":1e3}}}}";
        org.zstack.kvm.KVMHostAsyncHttpCallReply boundaryReply =
                (org.zstack.kvm.KVMHostAsyncHttpCallReply)
                        org.zstack.core.cloudbus.CloudBusGson.fromJson(boundaryMessage);
        Map<?, ?> boundaryState = boundaryReply.toResponse(
                org.zstack.kvm.memory.MemoryAgentResponse.class).state;
        for (String key : Arrays.asList("safeMax", "safeMin", "negativeZero")) {
            assertEquals(key, Double.class, boundaryState.get(key).getClass());
        }
        for (String key : Arrays.asList("twoTo53", "twoTo53PlusTwo", "aboveSafe", "belowSafe",
                "longMax", "longMin")) {
            assertEquals(key, Long.class, boundaryState.get(key).getClass());
        }
        assertEquals(java.math.BigInteger.class, boundaryState.get("uint64").getClass());
        assertEquals(java.math.BigInteger.class, boundaryState.get("uint64Max").getClass());
        assertExactUnsignedInteger("9223372036854775808", boundaryState.get("uint64"));
        assertExactUnsignedInteger("18446744073709551615", boundaryState.get("uint64Max"));
        assertEquals(Double.class, boundaryState.get("uint64Overflow").getClass());
        assertEquals(Double.class, boundaryState.get("negativeOverflow").getClass());
        assertEquals(0, Double.compare(-0.0d, (Double) boundaryState.get("negativeZero")));

        TypedNumberFixture typed = org.zstack.core.cloudbus.CloudBusGson.fromJson(
                "{\"whole\":9223372036854775807,\"fraction\":1.25,\"exponent\":1e3}",
                TypedNumberFixture.class);
        assertEquals(Long.valueOf(Long.MAX_VALUE), typed.whole);
        assertEquals(Double.class, typed.fraction.getClass());
    }

    private static void assertExactUnsignedInteger(String expected, Object value) {
        assertTrue(value instanceof Number);
        assertFalse(value instanceof Double);
        assertFalse(value instanceof Float);
        assertEquals(new java.math.BigInteger(expected),
                new java.math.BigDecimal(value.toString()).toBigIntegerExact());
    }
    @Test public void allThreePolicyEntriesAllowScopeOmissionButRetainExplicitCompatibilityValues() throws Exception {
        for (Class<?> type : Arrays.asList(APIGetMemoryPolicyMsg.class, APIPreviewMemoryPolicyMsg.class,
                APIUpdateMemoryPolicyMsg.class)) {
            org.zstack.header.message.APIParam param = type.getDeclaredField("scope")
                    .getAnnotation(org.zstack.header.message.APIParam.class);
            assertFalse(type.getSimpleName(), param.required());
            assertEquals(new HashSet<>(Arrays.asList("Global", "Cluster", "Host", "VM")),
                    new HashSet<>(Arrays.asList(param.validValues())));
        }
        assertNull(APIGetMemoryPolicyMsg.__example__().getScope());
        assertNull(APIPreviewMemoryPolicyMsg.__example__().getScope());
        assertNull(APIUpdateMemoryPolicyMsg.__example__().getScope());
    }
    @Test public void perVmAndOperationQueriesAreReadOnlyRoutes() throws Exception {
        String[] names = {"APIGetVmMemoryOptimizationMsg", "APIQueryHostMemoryOperationsMsg"};
        String[] paths = {"/vm-instances/{vmUuid}/memory-optimization", "/hosts/{hostUuid}/memory-operations"};
        for (int i = 0; i < names.length; i++) {
            Class<?> type = Class.forName("org.zstack.kvm.memory." + names[i]);
            RestRequest route = type.getAnnotation(RestRequest.class);
            assertEquals(HttpMethod.GET, route.method()); assertEquals(paths[i], route.path());
        }
    }

    @Test public void paginationExposesNextPageWithoutLosingLargeCollections() throws Exception {
        for (Class<?> type : Arrays.asList(APIQueryMemoryStateReply.class, APIQueryMemoryTaskReply.class)) {
            assertEquals(Integer.class, type.getMethod("getNextPage").getReturnType());
            assertEquals(String.class, type.getMethod("getSnapshotId").getReturnType());
        }
        assertEquals(String.class, APIQueryMemoryStateMsg.class.getMethod("getSnapshotId").getReturnType());
        assertEquals(String.class, APIQueryMemoryTaskMsg.class.getMethod("getSnapshotId").getReturnType());
        assertEquals(Integer.valueOf(500), MemoryOptimizationManager.nextPage(0, 500, 501));
        assertNull(MemoryOptimizationManager.nextPage(500, 1, 501));
        assertNull(MemoryOptimizationManager.nextPage(0, 0, 100));
        assertEquals(MemoryVmAccountingInventory.class, APIGetVmMemoryOptimizationReply.class.getMethod("getInventory").getReturnType());
        java.lang.reflect.Type batch = APIGetVmMemoryOptimizationsReply.class.getDeclaredField("inventories").getGenericType();
        assertTrue(batch.getTypeName(), batch.getTypeName().contains("MemoryVmAccountingInventory"));
        assertEquals(MemoryHostOperationsInventory.class, APIQueryHostMemoryOperationsReply.class.getMethod("getInventory").getReturnType());
    }

    @Test public void typedVmReplyUsesCanonicalNamesBoundedAliasesAndNullableMetricTypes() {
        MemoryVmAccountingInventory vm=new MemoryVmAccountingInventory(); vm.setVmUuid("vm"); vm.setQuality("Fresh");
        MemoryVmAccountingMetricsInventory m=new MemoryVmAccountingMetricsInventory(); m.setZeroPages(0L); m.setRamOriginalBytes(4096L); vm.setMetrics(m);
        APIGetVmMemoryOptimizationReply reply=new APIGetVmMemoryOptimizationReply(); reply.setInventory(vm);
        com.google.gson.JsonObject json=new com.google.gson.Gson().toJsonTree(reply).getAsJsonObject();
        com.google.gson.JsonObject metrics=json.getAsJsonObject("inventory").getAsJsonObject("metrics");
        assertEquals(0L,metrics.get("zeroPages").getAsLong()); assertEquals(4096L,metrics.get("ramOriginalBytes").getAsLong());
        assertEquals(metrics.get("ramOriginalBytes"), metrics.get("ram_original_bytes"));
        assertFalse(metrics.has("zero_pages"));
        assertTrue(!metrics.has("compressedPages") || metrics.get("compressedPages").isJsonNull()); assertFalse(json.toString().contains("unmodeledAgentField"));
    }

    @Test public void prometheusManagerCallbackDoesNotRequireAgentHostUuidEnvelope() {
        MemoryAgentResponse response = new MemoryAgentResponse();
        response.setSuccess(true);
        response.state = new LinkedHashMap<>();
        response.state.put("observed_at", java.time.Instant.now().toString());
        Map<String,Object> vm = new LinkedHashMap<>();
        vm.put("quality", "source_memcg_inventory");
        vm.put("metrics", Collections.singletonMap("ram_original_bytes", 4096L));
        response.state.put("vms", Collections.singletonMap("vm", vm));
        // callVmMetrics is the Prometheus provider path: its response has no Agent envelope hostUuid.
        assertNull(response.hostUuid);
        MemoryVmAccountingInventory inventory = MemoryOptimizationManager.vmObservation("host", "vm", response);
        assertEquals("host", inventory.getHostUuid());
        assertEquals(Long.valueOf(4096L), inventory.getMetrics().getRam_original_bytes());
    }

    @Test public void vmQueryKeepsUnknownAndNeverLeaksOtherVmRows() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("quality", "atomic_inventory_identity_checked");
        report.put("observed_at", java.time.Instant.now().toString());
        report.put("vms", Collections.singletonMap("other-vm", Collections.singletonMap("metrics", 99)));
        MemoryVmAccountingInventory unavailable = MemoryObservationRules.vm("host", "vm1", report, System.currentTimeMillis(), 90000L);
        assertEquals("unavailable", unavailable.getQuality());
        assertNull(unavailable.getMetrics());
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("quality", "source_memcg_inventory");
        record.put("metrics", Collections.singletonMap("ram_original_bytes", 4096));
        report.put("vms", Collections.singletonMap("vm1", record));
        Map<String,Object> metrics=new LinkedHashMap<>(); metrics.put("ram_original_bytes",4096L); record.put("metrics",metrics);
        Map<String,Object> identity=new LinkedHashMap<>(); identity.put("uuid","vm1"); record.put("identity",identity);
        Map<String, Object> present = new LinkedHashMap<>(); present.put("vm1",record); report.put("vms",present);
        MemoryVmAccountingInventory typed = MemoryObservationRules.vm("host", "vm1", report, System.currentTimeMillis(), 90000L);
        assertEquals(Long.valueOf(4096L), typed.getMetrics().getRam_original_bytes());
        assertEquals("source_memcg_inventory", typed.getQuality());
    }

    @Test public void expiredVmSamplesAreNotRenewedByQueryOrFilledWithZero() {
        Map<String, Object> report = new LinkedHashMap<>();
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("quality", "source_memcg_inventory");
        record.put("metrics", Collections.singletonMap("ram_original_bytes", 4096));
        report.put("vms", Collections.singletonMap("vm", record));
        report.put("observed_at", "2026-10-07T00:00:00Z");
        long observed = java.time.Instant.parse((String) report.get("observed_at")).toEpochMilli();
        assertNotNull(MemoryObservationRules.vm("host", "vm", report, observed + 89999, 90000).getMetrics());
        MemoryVmAccountingInventory expired = MemoryObservationRules.vm("host", "vm", report, observed + 90000, 90000);
        assertEquals("stale", expired.getQuality()); assertNull(expired.getMetrics());
        assertEquals(report.get("observed_at"), expired.getObserved_at());
        assertNull(MemoryObservationRules.vm("host", "vm", report, observed - 5001, 90000).getMetrics());
        report.remove("observed_at");
        assertEquals("unavailable", MemoryObservationRules.vm("host", "vm", report, observed, 90000).getQuality());
    }

    @Test public void pausedStateExposesResumeFenceAndAllowedAction() {
        MemoryStateVO state = new MemoryStateVO();
        state.setHostUuid("host"); state.setStatus("Succeeded");
        state.setControlOperationUuid("pause-operation"); state.setLastSampleTime(System.currentTimeMillis());
        state.setState("{\"lifecycle\":{\"paused\":true,\"activeState\":\"paused\"}}");
        MemoryStateInventory inventory = state.toInventory();
        assertEquals("pause-operation", inventory.getControlOperationUuid());
        // A plain state row cannot establish which durable operation owns the
        // fence; the API service adds resume only after checking the task row.
        assertEquals(Collections.singletonList("query"), inventory.getAllowedActions());
    }
}
