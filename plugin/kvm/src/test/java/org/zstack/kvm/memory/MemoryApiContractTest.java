package org.zstack.kvm.memory;

import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.rest.RESTConstant;
import org.zstack.rest.RestServer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

public class MemoryApiContractTest {
    @Test public void allPolicyEndpointsAcceptClusterScope() throws Exception {
        for (Class<?> type : new Class<?>[]{APIGetMemoryPolicyMsg.class,
                APIPreviewMemoryPolicyMsg.class, APIUpdateMemoryPolicyMsg.class}) {
            org.zstack.header.message.APIParam parameter = type.getDeclaredField("scope")
                    .getAnnotation(org.zstack.header.message.APIParam.class);
            assertTrue(type.getSimpleName(), java.util.Arrays.asList(parameter.validValues()).contains("Cluster"));
        }
    }
    @Test public void allMemoryApisPassActualRestServerRegistrationValidation() throws Exception {
        Method validate = RestServer.class.getDeclaredMethod("collectRestRequestErrConfigApi",
                List.class, Class.class, RestRequest.class);
        validate.setAccessible(true);
        List<String> errors = new ArrayList<>();
        RestServer server = new RestServer();
        String[] names = {"APIQueryMemoryStateMsg", "APIGetMemoryStatesMsg", "APIGetMemoryPolicyMsg",
                "APIUpdateMemoryPolicyMsg", "APIPreviewMemoryPolicyMsg", "APIQueryMemoryTaskMsg",
                "APIGetMemoryTasksMsg", "APIGetHostMemoryOperationsMsg", "APIGetMemorySummaryMsg",
                "APICancelMemoryTaskMsg", "APIDeleteMemoryTaskMsg", "APIGetVmMemoryOptimizationsMsg"};
        for (String name : names) {
            Class<?> type = api(name);
            validate.invoke(server, errors, type, type.getAnnotation(RestRequest.class));
        }
        assertTrue("Real RestServer bootstrap rejects APIs: " + errors, errors.isEmpty());
    }

    @Test public void previewExposesPerHostEligibilityAsTopLevelResponseField() throws Exception {
        RestResponse response = APIPreviewMemoryPolicyReply.class.getAnnotation(RestResponse.class);
        assertTrue(java.util.Arrays.asList(response.fieldsTo()).contains("hostResults"));
        assertTrue(java.util.Arrays.asList(response.fieldsTo()).contains("warningDetails"));
        assertNotNull(APIPreviewMemoryPolicyReply.class.getDeclaredField("hostResults"));
        assertNotNull(APIPreviewMemoryPolicyReply.class.getDeclaredField("warningDetails"));
        assertNotNull(MemoryHostPolicyPreviewInventory.class.getDeclaredField("hostUuid"));
        assertNotNull(MemoryHostPolicyPreviewInventory.class.getDeclaredField("eligible"));
        assertNotNull(MemoryHostPolicyPreviewInventory.class.getDeclaredField("blockedFields"));
        assertNotNull(MemoryBlockedPolicyFieldInventory.class.getDeclaredField("field"));
        assertNotNull(MemoryBlockedPolicyFieldInventory.class.getDeclaredField("reasonCode"));
    }

    @Test public void policyInventoryExposesPreflightRequiredActions() throws Exception {
        assertNotNull(MemoryPolicyInventory.class.getDeclaredField("preflightRequiredActions"));
        assertNotNull(MemoryPolicyInventory.class.getDeclaredMethod("getPreflightRequiredActions"));
    }

    @Test public void globalPolicyReadCanDiagnosePendingFreshCloudBootstrap() throws Exception {
        assertNotNull(MemoryPolicyInventory.class.getDeclaredField("bootstrapStatus"));
        assertNotNull(MemoryPolicyInventory.class.getDeclaredField("bootstrapReason"));
        assertNotNull(MemoryPolicyInventory.class.getDeclaredMethod("getBootstrapStatus"));
        assertNotNull(MemoryPolicyInventory.class.getDeclaredMethod("getBootstrapReason"));
    }

    @Test public void stateAndTaskPlanEvidenceAppearsInActualInventoryJson() {
        MemoryStateInventory state = new MemoryStateInventory();
        state.setPolicyPlanStatus("NeedsReview");
        state.setPolicyBlockedFields("{\"policy\":{\"reason\":\"LEGACY_APPLIED_POLICY_UNKNOWN\"}}");
        state.setAppliedPolicy("{\"schemaVersion\":1,\"ksm\":{\"enabled\":false}}");
        state.setAppliedPolicyHash("applied-hash");
        String stateJson = org.zstack.utils.gson.JSONObjectUtil.toJsonString(state);
        com.google.gson.JsonObject stateWire = new com.google.gson.JsonParser().parse(stateJson).getAsJsonObject();
        assertEquals("NeedsReview", stateWire.get("policyPlanStatus").getAsString());
        assertTrue(stateWire.get("policyBlockedFields").getAsString().contains("LEGACY_APPLIED_POLICY_UNKNOWN"));
        assertEquals(state.getAppliedPolicy(), stateWire.get("appliedPolicy").getAsString());

        MemoryTaskInventory task = new MemoryTaskInventory(); task.setPolicyPlanHash("plan-hash");
        String taskJson = org.zstack.utils.gson.JSONObjectUtil.toJsonString(task);
        assertEquals("plan-hash", new com.google.gson.JsonParser().parse(taskJson).getAsJsonObject()
                .get("policyPlanHash").getAsString());
    }

    @Test public void putActionsUseFrameworkDerivedEnvelopeNames() {
        for (Class<?> type : new Class<?>[]{APIUpdateMemoryPolicyMsg.class, APICancelMemoryTaskMsg.class}) {
            RestRequest request = type.getAnnotation(RestRequest.class);
            assertTrue(type.getSimpleName(), request.isAction());
            assertEquals(RESTConstant.DEFAULT_PARAMETER_NAME, request.parameterName());
            assertEquals(HttpMethod.PUT, request.method());
        }
    }

    @Test public void deleteTaskUsesExplicitAdminOnlyRestDeleteContract() throws Exception {
        RestRequest request = APIDeleteMemoryTaskMsg.class.getAnnotation(RestRequest.class);
        assertNotNull(request);
        assertEquals("/memory-optimization/tasks/{uuid}", request.path());
        assertEquals(HttpMethod.DELETE, request.method());
        assertFalse(request.isAction());
        assertTrue(APIDeleteMemoryTaskMsg.class.getAnnotation(org.zstack.header.identity.Action.class).adminOnly());
        assertEquals(32, APIDeleteMemoryTaskMsg.class.getDeclaredField("uuid")
                .getAnnotation(org.zstack.header.message.APIParam.class).maxLength());
        RestResponse response = APIDeleteMemoryTaskEvent.class.getAnnotation(RestResponse.class);
        assertTrue(java.util.Arrays.asList(response.fieldsTo()).containsAll(java.util.Arrays.asList(
                "taskUuid", "deleted", "scope", "resourceUuid", "hostUuids")));
        assertTrue(org.zstack.header.other.APIMultiAuditor.class.isAssignableFrom(APIDeleteMemoryTaskMsg.class));
    }

    @Test public void policyPayloadDoesNotUseTheObsoleteSixteenKiBAnnotationLimit() throws Exception {
        assertEquals(Integer.MIN_VALUE, APIUpdateMemoryPolicyMsg.class.getDeclaredField("policy")
                .getAnnotation(org.zstack.header.message.APIParam.class).maxLength());
        assertEquals(Integer.MIN_VALUE, APIPreviewMemoryPolicyMsg.class.getDeclaredField("policy")
                .getAnnotation(org.zstack.header.message.APIParam.class).maxLength());
    }

    @Test public void resumeIsASeparateFencedLifecycleAction() throws Exception {
        org.zstack.header.message.APIParam action = APIUpdateMemoryPolicyMsg.class
                .getDeclaredField("action").getAnnotation(org.zstack.header.message.APIParam.class);
        assertTrue(java.util.Arrays.asList(action.validValues()).contains("resume"));
        assertEquals(32, APIUpdateMemoryPolicyMsg.class.getDeclaredField("expectedControlOperationUuid")
                .getAnnotation(org.zstack.header.message.APIParam.class).maxLength());
    }

    @Test public void policyBudgetCountsUtf8BytesAndAllowsPayloadsOverSixteenKiB() {
        String payload = "{\"zram\":{\"algorithm\":\"" + repeat("界", 9000) + "\"}}";
        assertTrue(payload.length() > 16384 || payload.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 16384);
        MemoryApiRequestBudget.validatePolicy(payload, payload.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
    }

    @Test(expected = MemoryOperationException.class)
    public void policyBudgetRejectsOverConfiguredUtf8Bytes() {
        MemoryApiRequestBudget.validatePolicy("界界", 5);
    }

    @Test public void updateBudgetIncludesShardAndSelectionFieldsButNotReassembledPolicy() {
        APIUpdateMemoryPolicyMsg msg = new APIUpdateMemoryPolicyMsg();
        msg.setPolicy("{}");
        msg.setTargetShardIndex(0);
        msg.setTargetShardCount(2);
        msg.setTargetVmUuids(java.util.Arrays.asList("01234567890123456789012345678901"));
        MemoryApiRequestBudget.validateUpdate(msg, 1024);

        String largeShard = "{\"zram\":{\"algorithm\":\"" + repeat("a", 700) + "\"}}";
        msg.setPolicy(largeShard);
        MemoryApiRequestBudget.validateUpdate(msg, 1024);
        // A policy assembled from several independently admitted shards is
        // validated by the shard protocol, not rejected here as one API body.
    }

    private static String repeat(String value, int count) {
        StringBuilder result = new StringBuilder(value.length() * count);
        for (int i = 0; i < count; i++) { result.append(value); }
        return result.toString();
    }
    private Class<?> api(String name) {
        try {
            return Class.forName("org.zstack.kvm.memory." + name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Missing product API: " + name, e);
        }
    }

    @Test public void restRoutesAndResponsesAreRegistered() {
        String[][] routes = {
            {"APIQueryMemoryStateMsg", "/memory-states", "GET"},
            {"APIGetMemoryStatesMsg", "/memory-optimization/states", "GET"},
            {"APIGetMemoryPolicyMsg", "/memory-policies/{resourceUuid}", "GET"},
            {"APIUpdateMemoryPolicyMsg", "/memory-policies/{resourceUuid}/actions", "PUT"},
            {"APIPreviewMemoryPolicyMsg", "/memory-policies/{resourceUuid}/preview", "POST"},
            {"APIQueryMemoryTaskMsg", "/memory-tasks", "GET"},
            {"APIGetMemoryTasksMsg", "/memory-optimization/tasks", "GET"},
            {"APIGetHostMemoryOperationsMsg", "/hosts/{hostUuid}/memory-optimization/operations", "GET"},
            {"APIGetMemorySummaryMsg", "/memory-summary", "GET"},
            {"APICancelMemoryTaskMsg", "/memory-optimization/tasks/{uuid}/actions", "PUT"},
            {"APIGetVmMemoryOptimizationsMsg", "/vm-instances/memory-optimizations", "GET"}
        };
        for (String[] route : routes) {
            RestRequest request = api(route[0]).getAnnotation(RestRequest.class);
            assertNotNull(request);
            assertEquals(route[1], request.path());
            assertEquals(HttpMethod.valueOf(route[2]), request.method());
            assertNotNull(request.responseClass().getAnnotation(RestResponse.class));
        }
    }

    @Test public void cancelKeepsLegacyRouteAsSameActionAlias() {
        RestRequest request = APICancelMemoryTaskMsg.class.getAnnotation(RestRequest.class);
        assertEquals("/memory-optimization/tasks/{uuid}/actions", request.path());
        assertArrayEquals(new String[]{"/memory-tasks/{uuid}/actions"}, request.optionalPaths());
        assertTrue(request.isAction());
        assertEquals(HttpMethod.PUT, request.method());
        assertEquals(APICancelMemoryTaskEvent.class, request.responseClass());
    }

    @Test public void summaryUsesStandardHostResourceValidationButHistoryDoesNot() throws Exception {
        assertEquals(org.zstack.header.host.HostVO.class, APIGetMemorySummaryMsg.class
                .getDeclaredField("hostUuids").getAnnotation(org.zstack.header.message.APIParam.class).resourceType());
        assertEquals(Object.class, APIQueryMemoryTaskMsg.class.getDeclaredField("hostUuid")
                .getAnnotation(org.zstack.header.message.APIParam.class).resourceType());
    }

    @Test public void getAliasesPreserveLegacyQueryFieldsAndReplyContracts() throws Exception {
        assertTrue(APIGetMemoryStatesMsg.class.getSuperclass() == APIQueryMemoryStateMsg.class);
        assertTrue(APIGetMemoryTasksMsg.class.getSuperclass() == APIQueryMemoryTaskMsg.class);
        assertTrue(APIGetHostMemoryOperationsMsg.class.getSuperclass() == APIQueryHostMemoryOperationsMsg.class);
        assertEquals(APIQueryMemoryStateReply.class, APIGetMemoryStatesMsg.class.getAnnotation(RestRequest.class).responseClass());
        assertEquals(APIQueryMemoryTaskReply.class, APIGetMemoryTasksMsg.class.getAnnotation(RestRequest.class).responseClass());
        assertEquals(APIQueryHostMemoryOperationsReply.class, APIGetHostMemoryOperationsMsg.class.getAnnotation(RestRequest.class).responseClass());
        assertTrue(APIGetMemoryStatesMsg.class.getMethod("getSnapshotId") != null);
        assertTrue(APIGetMemoryTasksMsg.class.getMethod("getSnapshotId") != null);
        assertTrue(APIGetHostMemoryOperationsMsg.class.getMethod("getStart") != null);
        assertTrue(APIGetMemoryStatesMsg.class.getAnnotation(org.zstack.header.identity.Action.class).adminOnly());
        assertTrue(APIGetMemoryTasksMsg.class.getAnnotation(org.zstack.header.identity.Action.class).adminOnly());
        assertTrue(APIGetHostMemoryOperationsMsg.class.getAnnotation(org.zstack.header.identity.Action.class).adminOnly());
        assertTrue(APIQueryMemoryStateMsg.class.isAnnotationPresent(Deprecated.class));
        assertTrue(APIQueryMemoryTaskMsg.class.isAnnotationPresent(Deprecated.class));
        assertTrue(APIQueryHostMemoryOperationsMsg.class.isAnnotationPresent(Deprecated.class));
    }

    @Test public void batchVmQueryRequiresNonemptyAccountCheckedVmUuids() throws Exception {
        org.zstack.header.message.APIParam parameter = APIGetVmMemoryOptimizationsMsg.class
                .getDeclaredField("vmUuids").getAnnotation(org.zstack.header.message.APIParam.class);
        assertTrue(parameter.nonempty());
        assertTrue(parameter.checkAccount());
    }

    @Test public void memoryReadAliasesAreDeclaredInMemoryServiceConfig() throws Exception {
        Path config = null;
        for (String candidate : new String[]{
                "conf/serviceConfig/memoryOptimization.xml",
                "../conf/serviceConfig/memoryOptimization.xml",
                "../../conf/serviceConfig/memoryOptimization.xml",
                "../../../conf/serviceConfig/memoryOptimization.xml"}) {
            Path path = Paths.get(candidate);
            if (Files.isRegularFile(path)) {
                config = path;
                break;
            }
        }
        assertNotNull("memoryOptimization service config is not available", config);
        String xml = new String(Files.readAllBytes(config), StandardCharsets.UTF_8);
        for (String name : new String[]{"APIGetVmMemoryOptimizationsMsg", "APIGetMemoryStatesMsg",
                "APIGetMemoryTasksMsg", "APIGetHostMemoryOperationsMsg", "APIDeleteMemoryTaskMsg"}) {
            assertTrue(name + " is not routed to memoryOptimization service",
                    xml.contains("org.zstack.kvm.memory." + name));
        }
    }

    @Test public void allPublicMemoryApiExamplesValidateAndResolveEveryPathVariable() throws Exception {
        Class<?>[] messages = {APICancelMemoryTaskMsg.class, APIDeleteMemoryTaskMsg.class, APIGetHostMemoryOperationsMsg.class,
                APIGetHostMemoryWritebackBackendsMsg.class, APIGetMemoryPolicyMsg.class,
                APIGetMemoryStatesMsg.class, APIGetMemorySummaryMsg.class, APIGetMemoryTasksMsg.class,
                APIGetVmMemoryOptimizationMsg.class, APIGetVmMemoryOptimizationsMsg.class,
                APIPreviewMemoryPolicyMsg.class, APIQueryHostMemoryOperationsMsg.class,
                APIQueryMemoryStateMsg.class, APIQueryMemoryTaskMsg.class, APIUpdateMemoryPolicyMsg.class};
        for (Class<?> type : messages) {
            RestRequest request = type.getAnnotation(RestRequest.class);
            assertNotNull(type.getSimpleName() + " missing REST route", request);
            Object example = type.getMethod("__example__").invoke(null);
            assertNotNull(type.getSimpleName() + " returned null example", example);
            ((org.zstack.header.message.APIMessage) example).validate();
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\{([^}]+)}")
                    .matcher(request.path());
            while (matcher.find()) {
                String variable = matcher.group(1);
                Object value = type.getMethod("get" + Character.toUpperCase(variable.charAt(0))
                        + variable.substring(1)).invoke(example);
                assertNotNull(type.getSimpleName() + " example leaves URL variable " + variable, value);
                assertFalse(type.getSimpleName() + " example has empty URL variable " + variable,
                        value.toString().trim().isEmpty());
            }
        }
    }

    @Test public void publicMemoryReplyEventAndInventoryExamplesAreNonempty() throws Exception {
        Class<?>[] examples = {APICancelMemoryTaskEvent.class, APIUpdateMemoryPolicyEvent.class,
                APIGetHostMemoryWritebackBackendsReply.class, APIGetMemoryPolicyReply.class,
                APIGetMemorySummaryReply.class, APIGetVmMemoryOptimizationReply.class,
                APIGetVmMemoryOptimizationsReply.class, APIPreviewMemoryPolicyReply.class,
                APIQueryHostMemoryOperationsReply.class, APIQueryMemoryStateReply.class,
                APIQueryMemoryTaskReply.class, MemoryBlockedPolicyFieldInventory.class,
                MemoryFieldCapability.class, MemoryHostPolicyPreviewInventory.class,
                MemoryPolicyInventory.class, MemoryPreviewWarningInventory.class,
                MemoryStateInventory.class, MemorySummaryInventory.class, MemoryTaskInventory.class,
                MemoryVmExclusionInventory.class};
        for (Class<?> type : examples) {
            Object value = type.getMethod("__example__").invoke(null);
            assertNotNull(type.getSimpleName() + " returned null example", value);
            String json = org.zstack.utils.gson.JSONObjectUtil.toJsonString(value);
            assertNotNull(type.getSimpleName() + " example did not serialize", json);
            assertTrue(type.getSimpleName() + " serialized as an empty object: " + json,
                    json.matches("(?s).*\\\"[A-Za-z][A-Za-z0-9]*\\\"\\s*:.*"));
        }
    }

    @Test public void officialMemoryApiDocTemplatesCoverMessagesRepliesAndReferencedInventories() {
        Path sourceRoot = null;
        for (String candidate : new String[]{"src/main/java/org/zstack/kvm/memory", "plugin/kvm/src/main/java/org/zstack/kvm/memory",
                "../plugin/kvm/src/main/java/org/zstack/kvm/memory",
                "../../plugin/kvm/src/main/java/org/zstack/kvm/memory"}) {
            Path path = Paths.get(candidate);
            if (Files.isDirectory(path)) { sourceRoot = path; break; }
        }
        assertNotNull("memory API source directory not found", sourceRoot);
        String[] templateNames = {"APICancelMemoryTaskMsg", "APIGetHostMemoryOperationsMsg",
                "APIGetHostMemoryWritebackBackendsMsg", "APIGetMemoryPolicyMsg", "APIGetMemoryStatesMsg",
                "APIGetMemorySummaryMsg", "APIGetMemoryTasksMsg", "APIGetVmMemoryOptimizationMsg",
                "APIGetVmMemoryOptimizationsMsg", "APIPreviewMemoryPolicyMsg", "APIQueryHostMemoryOperationsMsg",
                "APIQueryMemoryStateMsg", "APIQueryMemoryTaskMsg", "APIUpdateMemoryPolicyMsg",
                "APICancelMemoryTaskEvent", "APIUpdateMemoryPolicyEvent", "APIGetHostMemoryWritebackBackendsReply",
                "APIDeleteMemoryTaskMsg", "APIDeleteMemoryTaskEvent",
                "APIGetMemoryPolicyReply", "APIGetMemorySummaryReply", "APIGetVmMemoryOptimizationReply",
                "APIGetVmMemoryOptimizationsReply", "APIPreviewMemoryPolicyReply", "APIQueryHostMemoryOperationsReply",
                "APIQueryMemoryStateReply", "APIQueryMemoryTaskReply", "MemoryBlockedPolicyFieldInventory",
                "MemoryFieldCapability", "MemoryHostPolicyPreviewInventory", "MemoryPolicyInventory",
                "MemoryPreviewWarningInventory", "MemoryStateInventory", "MemorySummaryInventory",
                "MemoryTaskInventory", "MemoryVmExclusionInventory", "MemoryUncertainRecovery",
                "MemoryBackendPreparation", "MemoryZramPoolPreparation", "MemoryVmAccountingInventory",
                "MemoryVmAccountingMetricsInventory", "MemoryVmIdentityInventory", "MemoryHostOperationsInventory",
                "MemoryHostOperationsInventory_Entry", "MemoryHostOperationsInventory_Concurrency",
                "MemoryHostOperationsInventory_RecordBudget"};
        for (String name : templateNames) {
            Path file = sourceRoot.resolve(name + "Doc_zh_cn.groovy");
            assertTrue("missing generated official template " + file, Files.isRegularFile(file));
            try {
                String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                assertTrue(name + " missing current API version", text.contains("since \"5.5.38\""));
                assertFalse(name + " still has an unfilled generated placeholder",
                        text.contains("在这里填写API描述") || text.contains("在这里输入结构的名称"));
            } catch (java.io.IOException e) {
                throw new AssertionError("cannot read template " + file, e);
            }
        }
    }

    @Test public void all36OfficialMemoryDocTemplatesExecuteWithTheRealRestDsl() throws Exception {
        Path sourceRoot = null;
        for (String candidate : new String[]{"plugin/kvm/src/main/java/org/zstack/kvm/memory",
                "../plugin/kvm/src/main/java/org/zstack/kvm/memory",
                "../../plugin/kvm/src/main/java/org/zstack/kvm/memory"}) {
            Path path = Paths.get(candidate);
            if (Files.isDirectory(path)) { sourceRoot = path; break; }
        }
        assertNotNull("memory API source directory not found", sourceRoot);
        String[] names = {"APIGetHostMemoryOperationsMsg", "APIGetHostMemoryWritebackBackendsMsg",
                "APIGetMemoryPolicyMsg", "APIGetMemoryStatesMsg", "APIGetMemorySummaryMsg",
                "APIGetMemoryTasksMsg", "APIGetVmMemoryOptimizationMsg", "APIGetVmMemoryOptimizationsMsg",
                "APIPreviewMemoryPolicyMsg", "APIQueryHostMemoryOperationsMsg", "APIQueryMemoryStateMsg",
                "APIQueryMemoryTaskMsg", "APIUpdateMemoryPolicyMsg", "APICancelMemoryTaskMsg",
                "APIDeleteMemoryTaskMsg", "APICancelMemoryTaskEvent", "APIDeleteMemoryTaskEvent",
                "APIUpdateMemoryPolicyEvent", "APIGetHostMemoryWritebackBackendsReply", "APIGetMemoryPolicyReply",
                "APIGetMemorySummaryReply", "APIGetVmMemoryOptimizationReply", "APIGetVmMemoryOptimizationsReply",
                "APIPreviewMemoryPolicyReply", "APIQueryHostMemoryOperationsReply", "APIQueryMemoryStateReply",
                "APIQueryMemoryTaskReply", "MemoryBlockedPolicyFieldInventory", "MemoryFieldCapability",
                "MemoryHostPolicyPreviewInventory", "MemoryPolicyInventory", "MemoryPreviewWarningInventory",
                "MemoryStateInventory", "MemorySummaryInventory", "MemoryTaskInventory",
                "MemoryVmExclusionInventory"};
        assertEquals("test inventory must include every official memory API template", 36, names.length);

        Class<?> generatorClass = org.zstack.utils.GroovyUtils.getClass(
                "scripts/RestDocumentationGenerator.groovy", RestServer.class.getClassLoader());
        // The generator constructor eagerly scans every platform GlobalConfig.
        // KVM's focused test classpath intentionally lacks unrelated modules;
        // the actual createDoc DSL execution has no dependency on that scan.
        Object generator = allocateWithoutConstructor(generatorClass);
        Method createDoc = generator.getClass().getMethod("createDoc", String.class);
        Object deleteTaskDoc = null;
        for (String name : names) {
            Path template = sourceRoot.resolve(name + "Doc_zh_cn.groovy");
            assertTrue("missing official template " + template, Files.isRegularFile(template));
            try {
                Object doc = createDoc.invoke(generator, template.toAbsolutePath().toString());
                assertNotNull(name + " produced a null official Doc", doc);
                if ("APIDeleteMemoryTaskMsg".equals(name)) {
                    deleteTaskDoc = doc;
                }
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw new AssertionError(name + " failed to execute with the official REST documentation DSL",
                        e.getTargetException());
            }
        }
        assertNotNull("DeleteMemoryTask request template was not parsed", deleteTaskDoc);
        Object rest = declaredFieldValue(deleteTaskDoc, "_rest");
        Object response = declaredFieldValue(rest, "_response");
        assertEquals("DeleteMemoryTask response must use the API event class documented in EventDoc",
                APIDeleteMemoryTaskEvent.class, declaredFieldValue(response, "_clz"));
    }

    private static Object declaredFieldValue(Object target, String fieldName) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static Object allocateWithoutConstructor(Class<?> type) throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        java.lang.reflect.Field singleton = unsafeClass.getDeclaredField("theUnsafe");
        singleton.setAccessible(true);
        Object unsafe = singleton.get(null);
        return unsafeClass.getMethod("allocateInstance", Class.class).invoke(unsafe, type);
    }
}
