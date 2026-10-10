package org.zstack.kvm.memory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.header.identity.SessionInventory;
import org.zstack.utils.gson.JSONObjectUtil;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Published public-action examples are parsed from the checked-in JSON resource,
 * converted through the production Gson utility, and submitted to the real
 * MemoryRepository admission path using only the repository's private-H2 fixture.
 */
@org.junit.runner.RunWith(org.junit.runners.Parameterized.class)
public class MemoryUpdatePolicyActionExamplesTest {
    @org.junit.runners.Parameterized.Parameters(name = "omitCompatibilityScope={0}")
    public static Collection<Object[]> scopeModes() {
        return Arrays.asList(new Object[]{false}, new Object[]{true});
    }
    private final boolean omitCompatibilityScope;
    public MemoryUpdatePolicyActionExamplesTest(boolean omitCompatibilityScope) {
        this.omitCompatibilityScope = omitCompatibilityScope;
    }
    private static final String SAMPLE_HOST = "1234567890abcdef1234567890abcdef";
    private static final String SAMPLE_CLUSTER = "234567890abcdef1234567890abcdef1";
    private static final String FIXTURE_HOST = "host1";
    private static final String FIXTURE_CLUSTER = "cluster1";

    private MemoryRepositoryTest fixture;
    private MemoryRepository repository;
    private JsonObject examples;

    @Before public void setUp() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/memory-optimization/update-memory-policy-actions.json")) {
            assertNotNull("published JSON resource must be on the test classpath", stream);
            examples = new JsonParser().parse(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }
        fixture = new MemoryRepositoryTest();
        fixture.setup();
        Field field = MemoryRepositoryTest.class.getDeclaredField("repository");
        field.setAccessible(true);
        repository = (MemoryRepository) field.get(fixture);
        Method register = MemoryRepositoryTest.class.getDeclaredMethod("registerPolicyReadResource", String.class, String.class);
        register.setAccessible(true);
        register.invoke(fixture, "host1", "HostVO");
        register.invoke(fixture, "host2", "HostVO");
    }

    @After public void tearDown() throws Exception {
        if (fixture != null) { fixture.close(); }
    }

    private JsonObject body(String action) {
        for (JsonElement element : examples.getAsJsonArray("actions")) {
            JsonObject entry = element.getAsJsonObject();
            if (action.equals(entry.get("action").getAsString())) {
                return entry.getAsJsonObject("request").deepCopy();
            }
        }
        throw new AssertionError("missing checked-in request example for action=" + action);
    }

    private APIUpdateMemoryPolicyMsg parse(String action) {
        JsonObject request = body(action);
        assertEquals("sample action must match its request action", action, request.get("action").getAsString());
        bindGlobalHostTargets(request);
        if (omitCompatibilityScope) { request.remove("scope"); }
        APIUpdateMemoryPolicyMsg msg = JSONObjectUtil.toObject(request.toString(), APIUpdateMemoryPolicyMsg.class);
        assertNotNull("request must be deserialized by production JSONObjectUtil", msg);
        SessionInventory session = new SessionInventory();
        session.setAccountUuid("example-account"); session.setUserUuid("example-user");
        msg.setSession(session); // transport metadata only; request body is not supplemented.
        return msg;
    }

    private APIUpdateMemoryPolicyMsg parseHost(String action, String fixtureHost) {
        JsonObject request = body(action);
        assertEquals("Host", request.get("scope").getAsString());
        assertEquals(SAMPLE_HOST, request.get("resourceUuid").getAsString());
        request.addProperty("resourceUuid", fixtureHost); // explicit fixture-resource binding.
        bindGlobalHostTargets(request);
        bindHostSourceJson(request, fixtureHost);
        if (omitCompatibilityScope) { request.remove("scope"); }
        APIUpdateMemoryPolicyMsg msg = JSONObjectUtil.toObject(request.toString(), APIUpdateMemoryPolicyMsg.class);
        SessionInventory session = new SessionInventory();
        session.setAccountUuid("example-account"); session.setUserUuid("example-user");
        msg.setSession(session);
        return msg;
    }

    private void bindGlobalHostTargets(JsonObject request) {
        if (request.has("targetHostUuids")) {
            assertEquals("target host example must use the declared sample host", SAMPLE_HOST,
                    request.getAsJsonArray("targetHostUuids").get(0).getAsString());
            com.google.gson.JsonArray boundHosts = new com.google.gson.JsonArray();
            boundHosts.add(FIXTURE_HOST);
            request.add("targetHostUuids", boundHosts); // explicit sample Host -> private fixture Host mapping.
        }
    }

    private void bindFreshHostFence(APIUpdateMemoryPolicyMsg msg) {
        String previewAction = "apply".equals(msg.getAction()) || "clearOverride".equals(msg.getAction())
                ? msg.getAction() : "apply";
        MemoryPolicyInventory preview = repository.preview(msg.getScope(), msg.getResourceUuid(),
                msg.getPolicy(), previewAction, msg.getClearOverrideFields());
        msg.setExpectedRevision(preview.getRevision());
        if (msg.getExpectedSourceRevisions() != null) {
            Map<String, Long> sampleChain = msg.getExpectedSourceRevisions();
            Map<String, Long> current = preview.getSourceRevisions();
            assertEquals("sample must provide the complete mapped source chain", current.keySet(), sampleChain.keySet());
            msg.setExpectedSourceRevisions(current); // bind freshly observed values for this private fixture.
        }
    }

    private static void assertUuid(String value, String field) {
        assertNotNull(field, value);
        assertTrue(field + " must be a public UUID", MemoryPolicyRules.validUuid(value));
    }

    @Test public void everyCheckedInBodyHasActionShapeAndValidPublicUuids() {
        assertEquals("memory-update-policy-action-examples-v1", examples.get("format").getAsString());
        Set<String> actions = new HashSet<>();
        for (JsonElement element : examples.getAsJsonArray("actions")) {
            JsonObject entry = element.getAsJsonObject();
            JsonObject request = entry.getAsJsonObject("request");
            String action = entry.get("action").getAsString();
            assertTrue("each action appears once", actions.add(action));
            assertEquals("entry and body action agree", action, request.get("action").getAsString());
            assertTrue("scope present", request.has("scope"));
            assertTrue("resource UUID present", request.has("resourceUuid"));
            assertTrue("client request UUID present", request.has("clientRequestUuid"));
            assertUuid(request.get("clientRequestUuid").getAsString(), action + ".clientRequestUuid");
            String scope = request.get("scope").getAsString();
            String resource = request.get("resourceUuid").getAsString();
            if ("Global".equals(scope)) { assertEquals("global", resource); }
            else { assertUuid(resource, action + ".resourceUuid"); }
            if (request.has("expectedControlOperationUuid")) {
                assertUuid(request.get("expectedControlOperationUuid").getAsString(), action + ".expectedControlOperationUuid");
            }
            if (request.has("targetHostUuids")) {
                assertEquals(1, request.getAsJsonArray("targetHostUuids").size());
                assertUuid(request.getAsJsonArray("targetHostUuids").get(0).getAsString(), action + ".targetHostUuid");
            }
            if (request.has("targetVmUuids")) {
                for (JsonElement vm : request.getAsJsonArray("targetVmUuids")) {
                    assertUuid(vm.getAsString(), action + ".targetVmUuid");
                }
            }
            if (request.has("recovery")) {
                JsonObject recovery = request.getAsJsonObject("recovery");
                assertUuid(recovery.get("expectedHostBootId").getAsString(), action + ".recovery.expectedHostBootId");
                assertUuid(recovery.get("drainControlOperationUuid").getAsString(), action + ".recovery.drainControlOperationUuid");
            }
            if (request.has("backendPreparation")) {
                assertUuid(request.getAsJsonObject("backendPreparation").get("expectedHostBootId").getAsString(),
                        action + ".backendPreparation.expectedHostBootId");
            }
            if (request.has("poolPreparation")) {
                assertUuid(request.getAsJsonObject("poolPreparation").get("expectedHostBootId").getAsString(),
                        action + ".poolPreparation.expectedHostBootId");
            }
            if (request.has("expectedSourceRevisions")) {
                for (Map.Entry<String, JsonElement> source : request.getAsJsonObject("expectedSourceRevisions").entrySet()) {
                    String[] parts = source.getKey().split(":", 2);
                    assertEquals("source revision key must be scope:uuid", 2, parts.length);
                    if (!"Global".equals(parts[0])) { assertUuid(parts[1], action + ".sourceRevision"); }
                    assertTrue("revision is non-negative", source.getValue().getAsLong() >= 0);
                }
            }
        }
        assertEquals(new HashSet<>(Arrays.asList("apply", "clearOverride", "pause", "drain", "resume", "reconcile",
                "recoverUncertain", "prepareWritebackBackend", "prepareZramPool", "stageTargetShard",
                "commitTargetShards", "cancelTargetShards")), actions);
    }

    @Test public void applyExampleIsAdmittedAgainstCurrentSourceChain() {
        APIUpdateMemoryPolicyMsg apply = parseHost("apply", FIXTURE_HOST);
        bindFreshHostFence(apply);
        assertEquals("Queued", repository.submit(apply).getStatus());
        assertEquals("apply", tasks(FIXTURE_HOST).get(0).getAction());
    }

    @Test public void clearOverrideExampleIsAdmittedAgainstCurrentSourceChain() {
        APIUpdateMemoryPolicyMsg clear = parseHost("clearOverride", FIXTURE_HOST);
        bindFreshHostFence(clear);
        assertEquals("Queued", repository.submit(clear).getStatus());
        assertEquals("clearOverride", tasks(FIXTURE_HOST).get(0).getAction());
    }

    @Test public void pauseAndDrainExamplesAreAdmittedAsConfigurationFreeHostActions() {
        APIUpdateMemoryPolicyMsg pause = parseHost("pause", FIXTURE_HOST);
        assertEquals("Queued", repository.submit(pause).getStatus());
        APIUpdateMemoryPolicyMsg drain = parseHost("drain", "host2"); // explicit second fixture Host mapping.
        assertEquals("Queued", repository.submit(drain).getStatus());
        assertEquals("pause", tasks(FIXTURE_HOST).get(0).getAction());
        assertEquals("drain", tasks("host2").get(0).getAction());
    }

    @Test public void resumeExampleUsesTheLiveSuccessfulPauseFence() {
        APIUpdateMemoryPolicyMsg pause = parseHost("pause", FIXTURE_HOST);
        repository.submit(pause);
        MemoryTaskVO task = repository.claim("a03-mn").get(0);
        MemoryAgentResponse paused = new MemoryAgentResponse();
        paused.status = "Succeeded"; paused.sampleTime = System.currentTimeMillis();
        paused.state = new HashMap<>(); paused.state.put("phase", "PAUSED");
        repository.result(task.getUuid(), "Succeeded", null, paused);
        String fence = repository.states(Collections.singletonList(FIXTURE_HOST), 0, 1).get(0).getControlOperationUuid();
        APIUpdateMemoryPolicyMsg resume = parseHost("resume", FIXTURE_HOST);
        resume.setExpectedControlOperationUuid(fence); // refresh only this explicitly time-varying fence.
        MemoryTaskInventory accepted = repository.submit(resume);
        assertEquals("Queued", accepted.getStatus());
        assertEquals("identical resume retries return the original task", accepted.getUuid(), repository.submit(resume).getUuid());
        resume.setExpectedControlOperationUuid("ffffffffffffffffffffffffffffffff");
        try { repository.submit(resume); fail("a changed control fence cannot replay the original request"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_IDEMPOTENCY_CONFLICT", expected.getCode()); }
    }

    @Test public void reconcileExampleIsAdmittedForExistingUnknownOperationWithoutReplay() {
        repository.submit(parseHost("pause", FIXTURE_HOST));
        repository.claim("a03-mn");
        repository.expireUnconfirmed("a03-mn");
        assertEquals("Queued", repository.submit(parseHost("reconcile", FIXTURE_HOST)).getStatus());
        List<MemoryTaskInventory> tasks = tasks(FIXTURE_HOST);
        assertTrue(tasks.stream().anyMatch(task -> "Unknown".equals(task.getStatus()) && "pause".equals(task.getAction())));
        assertTrue(tasks.stream().anyMatch(task -> "reconcile".equals(task.getAction())));
    }

    @Test public void writebackPreparationExampleIsAdmittedWithExplicitCurrentSourceFence() {
        APIUpdateMemoryPolicyMsg msg = parseHost("prepareWritebackBackend", FIXTURE_HOST);
        bindFreshHostFence(msg);
        assertEquals("Queued", repository.submit(msg).getStatus());
    }

    @Test public void prepareZramPoolExampleIsAdmittedWithRealCompletedDrainAndMaintenanceProof() throws Exception {
        String drainUuid = "44444444444444444444444444444444";
        String bootId = "1e73a145-2662-4911-9c4f-b75a7d91a88e";
        String generation = repeat('a', 64);
        fixtureDrainProof(drainUuid, bootId, generation);

        JsonObject request = body("prepareZramPool");
        request.addProperty("resourceUuid", FIXTURE_HOST);
        request.addProperty("expectedControlOperationUuid", drainUuid);
        request.getAsJsonObject("poolPreparation").addProperty("expectedHostBootId", bootId);
        request.getAsJsonObject("poolPreparation").addProperty("expectedPoolGeneration", generation);
        APIUpdateMemoryPolicyMsg msg = parseBoundBody("prepareZramPool", request);
        assertEquals("Queued", repository.submit(msg).getStatus());
        assertEquals("prepareZramPool", tasks(FIXTURE_HOST).get(0).getAction());
    }

    @Test public void recoverUncertainExampleIsAdmittedForTheExactLiveUnknownAndDrainFences() throws Exception {
        Method method = MemoryRepositoryTest.class.getDeclaredMethod("uncertainRecoveryRequest");
        method.setAccessible(true);
        APIUpdateMemoryPolicyMsg liveStateFixture = (APIUpdateMemoryPolicyMsg) method.invoke(fixture);
        JsonObject request = body("recoverUncertain");
        request.addProperty("resourceUuid", FIXTURE_HOST);
        request.addProperty("expectedControlOperationUuid", liveStateFixture.getExpectedControlOperationUuid());
        JsonObject recovery = request.getAsJsonObject("recovery");
        MemoryUncertainRecovery live = liveStateFixture.getRecovery();
        recovery.addProperty("expectedHostBootId", live.getExpectedHostBootId());
        recovery.addProperty("expectedPoolGeneration", live.getExpectedPoolGeneration());
        recovery.addProperty("drainControlOperationUuid", live.getDrainControlOperationUuid());
        assertTrue("example must explicitly confirm recovery", recovery.get("confirmed").getAsBoolean());
        APIUpdateMemoryPolicyMsg msg = parseBoundBody("recoverUncertain", request);
        assertEquals("Queued", repository.submit(msg).getStatus());
        assertEquals("recoverUncertain", tasks(FIXTURE_HOST).get(0).getAction());
    }

    private APIUpdateMemoryPolicyMsg parseBoundBody(String action, JsonObject request) {
        assertEquals(action, request.get("action").getAsString());
        if ("Host".equals(request.get("scope").getAsString())) {
            bindHostSourceJson(request, request.get("resourceUuid").getAsString());
        }
        if (omitCompatibilityScope) { request.remove("scope"); }
        APIUpdateMemoryPolicyMsg msg = JSONObjectUtil.toObject(request.toString(), APIUpdateMemoryPolicyMsg.class);
        SessionInventory session = new SessionInventory();
        session.setAccountUuid("example-account"); session.setUserUuid("example-user");
        msg.setSession(session);
        if (msg.getExpectedSourceRevisions() != null) { bindFreshHostFence(msg); }
        return msg;
    }

    private void bindHostSourceJson(JsonObject request, String fixtureHost) {
        if (!request.has("expectedSourceRevisions")) { return; }
        JsonObject sample = request.getAsJsonObject("expectedSourceRevisions");
        assertTrue("example must carry Global source", sample.has("Global:global"));
        assertTrue("example must carry Cluster source", sample.has("Cluster:" + SAMPLE_CLUSTER));
        assertTrue("example must carry Host source", sample.has("Host:" + SAMPLE_HOST));
        JsonObject bound = new JsonObject();
        bound.addProperty("Global:global", sample.get("Global:global").getAsLong());
        bound.addProperty("Cluster:" + FIXTURE_CLUSTER, sample.get("Cluster:" + SAMPLE_CLUSTER).getAsLong());
        bound.addProperty("Host:" + fixtureHost, sample.get("Host:" + SAMPLE_HOST).getAsLong());
        request.add("expectedSourceRevisions", bound);
    }

    private void fixtureDrainProof(String drainUuid, String bootId, String generation) throws Exception {
        Method taskFactory = MemoryRepositoryTest.class.getDeclaredMethod("testTask", String.class, String.class,
                String.class, String.class, String.class);
        taskFactory.setAccessible(true);
        MemoryTaskVO drain = (MemoryTaskVO) taskFactory.invoke(fixture, drainUuid, null, "drain", "Succeeded", null);
        repository.transaction(em -> {
            em.persist(drain);
            MemoryStateVO state = em.find(MemoryStateVO.class, FIXTURE_HOST);
            state.setStatus("Succeeded"); state.setControlOperationUuid(drainUuid);
            state.setLastSampleTime(System.currentTimeMillis());
            state.setState("{\"bootId\":\"" + bootId + "\",\"lastConfirmedOperationUuid\":\"" + drainUuid
                    + "\",\"maintenanceProof\":{\"maintenanceReady\":true,\"maintenanceArchived\":true,"
                    + "\"executorExited\":true,\"drainOperationUuid\":\"" + drainUuid + "\","
                    + "\"oldPoolGeneration\":\"" + generation + "\",\"activeOperations\":0,"
                    + "\"readyForInitialization\":true,\"originalDeviceInactive\":true,"
                    + "\"oldPoolOwnershipAbsent\":true}}");
            return null;
        });
    }

    private static String repeat(char value, int count) {
        char[] chars = new char[count]; Arrays.fill(chars, value); return new String(chars);
    }

    @Test public void targetShardStageAndCancelExamplesUseTheSameRequestEnvelope() {
        APIUpdateMemoryPolicyMsg stage = parse("stageTargetShard");
        assertEquals("global", stage.getResourceUuid());
        assertEquals("Staged", repository.submit(stage).getStatus());
        APIUpdateMemoryPolicyMsg cancel = parse("cancelTargetShards");
        assertEquals("global", cancel.getResourceUuid());
        assertEquals("Cancelled", repository.submit(cancel).getStatus());
    }

    @Test public void targetShardStageAndCommitExamplesAssembleAndCommitTheStagedTargets() {
        APIUpdateMemoryPolicyMsg stage = parse("stageTargetShard");
        assertEquals("Staged", repository.submit(stage).getStatus());
        APIUpdateMemoryPolicyMsg commit = parse("commitTargetShards");
        assertEquals(stage.getClientRequestUuid(), commit.getClientRequestUuid());
        assertEquals(stage.getTargetSnapshotGeneration(), commit.getTargetSnapshotGeneration());
        MemoryTaskInventory committed = repository.submit(commit);
        assertEquals("Queued", committed.getStatus());
        assertEquals("apply", committed.getAction());
        assertTrue("commit must consume staged target UUIDs", committed.getUuid() != null);
    }

    @Test public void rejectsMissingSourcesWrongLayerMixedMaintenanceAndIllegalSafetyFieldsFromExamples() {
        JsonObject missingBody = body("apply");
        missingBody.remove("expectedSourceRevisions");
        APIUpdateMemoryPolicyMsg missing = JSONObjectUtil.toObject(missingBody.toString(), APIUpdateMemoryPolicyMsg.class);
        SessionInventory session = new SessionInventory(); session.setAccountUuid("example-account"); session.setUserUuid("example-user"); missing.setSession(session);
        missing.setResourceUuid(FIXTURE_HOST);
        try { repository.submit(missing); fail("Host updates require the complete source revision snapshot"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_REVISION_CONFLICT", expected.getCode()); }

        APIUpdateMemoryPolicyMsg wrongLayer = parseHost("apply", FIXTURE_HOST);
        bindFreshHostFence(wrongLayer);
        wrongLayer.setExpectedSourceRevisions(Collections.singletonMap("Global:global", 0L));
        try { repository.submit(wrongLayer); fail("must reject a partial/wrong-layer chain"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_REVISION_CONFLICT", expected.getCode()); }

        APIUpdateMemoryPolicyMsg mixed = parseHost("prepareWritebackBackend", FIXTURE_HOST);
        bindFreshHostFence(mixed);
        MemoryZramPoolPreparation mixedPool = new MemoryZramPoolPreparation();
        mixedPool.setResetConfirmed(true);
        mixed.setPoolPreparation(mixedPool);
        try { repository.submit(mixed); fail("maintenance payload objects must not be mixed"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_ZRAM_POOL_PREPARATION_INVALID", expected.getCode()); }

        APIUpdateMemoryPolicyMsg illegal = parseHost("pause", FIXTURE_HOST);
        illegal.setPolicy("{\"ksm\":{\"enabled\":true}}");
        try { repository.submit(illegal); fail("safety action cannot smuggle policy fields"); }
        catch (MemoryOperationException expected) { assertEquals("MEMORY_INVALID_ACTION", expected.getCode()); }
    }

    @Test public void everyActionRejectsMissingResourceBeforeAnyTaskOrStagingWrite() {
        for (JsonElement entry : examples.getAsJsonArray("actions")) {
            String action = entry.getAsJsonObject().get("action").getAsString();
            APIUpdateMemoryPolicyMsg request = parse(action);
            request.setResourceUuid("ffffffffffffffffffffffffffffffff");
            if (!omitCompatibilityScope) { request.setScope("Host"); }
            try { repository.submit(request); fail("missing resource admitted for " + action); }
            catch (MemoryOperationException expected) { assertEquals(action, "MEMORY_INVALID_SCOPE", expected.getCode()); }
        }
        assertEquals(Long.valueOf(0), repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t", Long.class).getSingleResult()));
        assertEquals(Long.valueOf(0), repository.transaction(em -> em.createQuery(
                "select count(s) from MemoryTargetShardVO s", Long.class).getSingleResult()));
    }

    @Test public void everyActionRejectsInapplicableFieldsWithoutPartialPersistence() {
        for (JsonElement entry : examples.getAsJsonArray("actions")) {
            String action = entry.getAsJsonObject().get("action").getAsString();
            APIUpdateMemoryPolicyMsg request = "Global".equals(entry.getAsJsonObject().getAsJsonObject("request")
                    .get("scope").getAsString()) ? parse(action) : parseHost(action, FIXTURE_HOST);
            if ("clearOverride".equals(action)) { request.setTargetShardCount(1); }
            else { request.setClearOverrideFields(Collections.singletonList("ksm.enabled")); }
            try { repository.submit(request); fail("inapplicable field admitted for " + action); }
            catch (MemoryOperationException expected) { assertEquals(action, "MEMORY_INVALID_REQUEST", expected.getCode()); }
        }
        assertEquals(Long.valueOf(0), repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t", Long.class).getSingleResult()));
        assertEquals(Long.valueOf(0), repository.transaction(em -> em.createQuery(
                "select count(s) from MemoryTargetShardVO s", Long.class).getSingleResult()));
    }

    @Test public void everyActionRejectsWrongResourceTypeAndNonKvmBeforeStaging() throws Exception {
        Method register = MemoryRepositoryTest.class.getDeclaredMethod("registerPolicyReadResource", String.class, String.class);
        register.setAccessible(true);
        register.invoke(fixture, "zone-example", "ZoneVO");
        repository.transaction(em -> {
            MemoryRepositoryTest.TestHost host = em.find(MemoryRepositoryTest.TestHost.class, "host2");
            host.hypervisorType = "ESX"; em.merge(host); return null;
        });
        for (JsonElement entry : examples.getAsJsonArray("actions")) {
            String action = entry.getAsJsonObject().get("action").getAsString();
            for (String invalidResource : Arrays.asList("zone-example", "host2")) {
                APIUpdateMemoryPolicyMsg request = parse(action);
                request.setResourceUuid(invalidResource);
                if (!omitCompatibilityScope) { request.setScope("Host"); }
                try { repository.submit(request); fail(action + " accepted invalid resource " + invalidResource); }
                catch (MemoryOperationException expected) { assertEquals(action, "MEMORY_INVALID_SCOPE", expected.getCode()); }
            }
        }
        assertEquals(Long.valueOf(0), repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t", Long.class).getSingleResult()));
        assertEquals(Long.valueOf(0), repository.transaction(em -> em.createQuery(
                "select count(s) from MemoryTargetShardVO s", Long.class).getSingleResult()));
    }

    @Test public void eachInapplicableTopLevelParameterIsRejectedIndependently() {
        Map<String, java.util.function.Consumer<APIUpdateMemoryPolicyMsg>> parameters = new LinkedHashMap<>();
        parameters.put("targetSnapshotGeneration", msg -> msg.setTargetSnapshotGeneration("snapshot"));
        parameters.put("targetShardIndex", msg -> msg.setTargetShardIndex(0));
        parameters.put("targetShardCount", msg -> msg.setTargetShardCount(1));
        parameters.put("targetTotalCount", msg -> msg.setTargetTotalCount(1L));
        parameters.put("targetDigest", msg -> msg.setTargetDigest(repeat('a', 64)));
        parameters.put("targetVmUuids", msg -> msg.setTargetVmUuids(Collections.singletonList(SAMPLE_HOST)));
        parameters.put("clearOverrideFields", msg -> msg.setClearOverrideFields(Collections.singletonList("ksm.enabled")));
        parameters.put("expectedInstanceGeneration", msg -> msg.setExpectedInstanceGeneration("instance"));
        parameters.put("expectedControlOperationUuid", msg -> msg.setExpectedControlOperationUuid(SAMPLE_HOST));
        parameters.put("expectedSourceRevisions", msg -> msg.setExpectedSourceRevisions(Collections.singletonMap("Global:global", 0L)));
        parameters.put("expectedGlobalRevision", msg -> msg.setExpectedGlobalRevision(0L));
        parameters.put("targetHostUuids", msg -> msg.setTargetHostUuids(Collections.singletonList(FIXTURE_HOST)));
        for (JsonElement entry : examples.getAsJsonArray("actions")) {
            String action = entry.getAsJsonObject().get("action").getAsString();
            boolean stage = "stageTargetShard".equals(action), commit = "commitTargetShards".equals(action);
            boolean cancel = "cancelTargetShards".equals(action);
            for (Map.Entry<String, java.util.function.Consumer<APIUpdateMemoryPolicyMsg>> parameter : parameters.entrySet()) {
                String field = parameter.getKey();
                boolean shardField = Arrays.asList("targetSnapshotGeneration", "targetShardIndex", "targetShardCount",
                        "targetTotalCount", "targetDigest", "targetVmUuids").contains(field);
                boolean forbidden = shardField && (!stage && !commit || commit &&
                        ("targetShardIndex".equals(field) || "targetVmUuids".equals(field)))
                        || "clearOverrideFields".equals(field) && !"clearOverride".equals(action)
                        || "expectedInstanceGeneration".equals(field)
                        || "expectedControlOperationUuid".equals(field) && (stage || commit || cancel)
                        || ("expectedSourceRevisions".equals(field) || "expectedGlobalRevision".equals(field)) && (stage || cancel)
                        || "targetHostUuids".equals(field) && (stage || cancel);
                if (!forbidden) { continue; }
                APIUpdateMemoryPolicyMsg request = "Global".equals(entry.getAsJsonObject().getAsJsonObject("request")
                        .get("scope").getAsString()) ? parse(action) : parseHost(action, FIXTURE_HOST);
                parameter.getValue().accept(request);
                try { repository.submit(request); fail(action + " must reject " + field); }
                catch (MemoryOperationException expected) {
                    assertEquals(action + ":" + field, "MEMORY_INVALID_REQUEST", expected.getCode());
                }
            }
        }
        assertEquals(Long.valueOf(0), repository.transaction(em -> em.createQuery(
                "select count(t) from MemoryTaskVO t", Long.class).getSingleResult()));
        assertEquals(Long.valueOf(0), repository.transaction(em -> em.createQuery(
                "select count(s) from MemoryTargetShardVO s", Long.class).getSingleResult()));
        assertEquals(Long.valueOf(0), repository.transaction(em -> em.createQuery(
                "select count(p) from MemoryPolicyVO p where p.revision <> 0", Long.class).getSingleResult()));
    }

    private List<MemoryTaskInventory> tasks(String hostUuid) {
        APIQueryMemoryTaskMsg query = new APIQueryMemoryTaskMsg();
        query.setHostUuid(hostUuid); query.setStart(0); query.setLimit(100);
        return repository.tasks(query);
    }
}
