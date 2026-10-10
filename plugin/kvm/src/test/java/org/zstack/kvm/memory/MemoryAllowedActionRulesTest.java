package org.zstack.kvm.memory;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class MemoryAllowedActionRulesTest {
    private static final long NOW = 1000;

    @Test
    public void licensedGlobalActionsRemainTargetScopedAndRequirePreflight() {
        MemoryAllowedActionRules.Decision decision = MemoryAllowedActionRules.evaluate(
                true, "Global", Collections.emptyMap(), null, null, NOW);
        assertTrue(decision.getAllowedActions().containsAll(Arrays.asList(
                "preview", "apply", "pause", "drain", "resume", "reconcile")));
        assertTrue(decision.getPreflightRequiredActions().containsAll(Arrays.asList(
                "apply", "pause", "drain", "resume", "reconcile")));
        assertPreflightIsAllowedWorkflowSubset(decision);
        assertFalse(decision.getPreflightRequiredActions().contains("recoverUncertain"));
    }

    @Test
    public void clusterPolicySupportsOverrideButTargetActionsRemainPreflightScoped() {
        MemoryAllowedActionRules.Decision decision = MemoryAllowedActionRules.evaluate(
                true, "Cluster", Collections.emptyMap(), null, null, NOW);
        assertTrue(decision.getAllowedActions().contains("clearOverride"));
        assertTrue(decision.getPreflightRequiredActions().contains("clearOverride"));
        assertTrue(decision.getPreflightRequiredActions().contains("apply"));
        assertPreflightIsAllowedWorkflowSubset(decision);
    }

    @Test
    public void unlicensedScopeCannotAdvertiseMutationsOrPreflightActions() {
        MemoryAllowedActionRules.Decision decision = MemoryAllowedActionRules.evaluate(
                false, "Cluster", Collections.emptyMap(), null, null, NOW);
        assertEquals(Collections.singletonList("preview"), decision.getAllowedActions());
        assertTrue(decision.getPreflightRequiredActions().isEmpty());
    }

    @Test
    public void hostPreparationActionsRequireFreshSupportedCapabilityAndPreflight() {
        MemoryStateVO supported = state(900, "{\"supported\":true,\"zram\":true,\"writeback\":true}");
        MemoryAllowedActionRules.Decision decision = MemoryAllowedActionRules.evaluate(
                true, "Host", writableFields(), supported, view(supported, false, false), NOW);
        assertTrue(decision.getAllowedActions().contains("apply"));
        assertTrue(decision.getAllowedActions().contains(MemoryBackendPreparationRules.ACTION));
        assertTrue(decision.getAllowedActions().contains(MemoryZramPoolPreparationRules.ACTION));
        assertTrue(decision.getPreflightRequiredActions().contains(MemoryBackendPreparationRules.ACTION));
        assertTrue(decision.getPreflightRequiredActions().contains(MemoryZramPoolPreparationRules.ACTION));
        assertPreflightIsAllowedWorkflowSubset(decision);
    }

    @Test
    public void explicitUnsupportedCapabilityIsNotAdvertisedAsExecutableOrPreflightable() {
        MemoryStateVO unsupported = state(900,
                "{\"supported\":true,\"zram\":false,\"zramReasonCode\":\"KERNEL_UNSUPPORTED\",\"writeback\":false,\"writebackReasonCode\":\"DRIVER_UNSUPPORTED\"}");
        MemoryAllowedActionRules.Decision decision = MemoryAllowedActionRules.evaluate(
                true, "Host", writableFields(), unsupported, view(unsupported, false, false), NOW);
        assertFalse(decision.getAllowedActions().contains("apply"));
        assertFalse(decision.getPreflightRequiredActions().contains("apply"));
        assertFalse(decision.getAllowedActions().contains(MemoryBackendPreparationRules.ACTION));
        assertFalse(decision.getAllowedActions().contains(MemoryZramPoolPreparationRules.ACTION));
        assertFalse(decision.getPreflightRequiredActions().contains(MemoryBackendPreparationRules.ACTION));
        assertFalse(decision.getPreflightRequiredActions().contains(MemoryZramPoolPreparationRules.ACTION));
        assertFalse(decision.getAllowedActions().contains("apply"));
    }

    @Test
    public void missingOrStaleNativeEvidenceAllowsEntryButRequiresPreflight() {
        MemoryAllowedActionRules.Decision missing = MemoryAllowedActionRules.evaluate(
                true, "Host", writableFields(), null, null, NOW);
        assertTrue(missing.getAllowedActions().contains(MemoryBackendPreparationRules.ACTION));
        assertTrue(missing.getAllowedActions().contains("apply"));
        assertTrue(missing.getPreflightRequiredActions().contains("apply"));
        assertTrue(missing.getAllowedActions().contains(MemoryBackendPreparationRules.ACTION));
        assertTrue(missing.getAllowedActions().contains(MemoryZramPoolPreparationRules.ACTION));
        assertTrue(missing.getPreflightRequiredActions().contains(MemoryBackendPreparationRules.ACTION));
        assertTrue(missing.getPreflightRequiredActions().contains(MemoryZramPoolPreparationRules.ACTION));
        assertPreflightIsAllowedWorkflowSubset(missing);
        assertPreflightIsAllowedWorkflowSubset(missing);

        MemoryAllowedActionRules.Decision stale = MemoryAllowedActionRules.evaluate(
                true, "Host", writableFields(), state(1, "{\"supported\":true}"), null, 100000);
        assertTrue(stale.getPreflightRequiredActions().contains("apply"));
        assertTrue(stale.getPreflightRequiredActions().contains(MemoryBackendPreparationRules.ACTION));
        assertTrue(stale.getPreflightRequiredActions().contains(MemoryZramPoolPreparationRules.ACTION));

        MemoryStateVO staleExplicitUnsupported = state(1,
                "{\"supported\":true,\"zram\":false,\"writeback\":false}");
        MemoryAllowedActionRules.Decision staleUnsupported = MemoryAllowedActionRules.evaluate(
                true, "Host", writableFields(), staleExplicitUnsupported, null, 100000);
        assertTrue("Stale false capability is not an explicit current unsupported result",
                staleUnsupported.getPreflightRequiredActions().contains(MemoryBackendPreparationRules.ACTION));
        assertTrue(staleUnsupported.getPreflightRequiredActions().contains(MemoryZramPoolPreparationRules.ACTION));
        assertTrue(staleUnsupported.getAllowedActions().contains(MemoryBackendPreparationRules.ACTION));
        assertTrue(staleUnsupported.getAllowedActions().contains(MemoryZramPoolPreparationRules.ACTION));
        assertPreflightIsAllowedWorkflowSubset(staleUnsupported);
    }

    @Test
    public void hostWithNoWritableFieldsDoesNotAdvertiseApply() {
        Map<String, MemoryFieldCapability> fields = new LinkedHashMap<>();
        MemoryFieldCapability unsupported = new MemoryFieldCapability(false, true, "boolean", null, null);
        unsupported.reasonCode = "KERNEL_UNSUPPORTED";
        fields.put("zram.enabled", unsupported);
        MemoryAllowedActionRules.Decision decision = MemoryAllowedActionRules.evaluate(
                true, "Host", fields, state(900, "{\"supported\":false,\"reasonCode\":\"KERNEL_UNSUPPORTED\"}"), null, NOW);
        assertFalse(decision.getAllowedActions().contains("apply"));
        assertFalse(decision.getAllowedActions().contains("clearOverride"));
    }

    @Test
    public void vmScopeNeverReceivesHostMaintenanceActions() {
        Map<String, MemoryFieldCapability> fields = new LinkedHashMap<>();
        fields.put("participation", new MemoryFieldCapability(true, true, "enum", null, "InstanceFenced"));
        MemoryAllowedActionRules.Decision decision = MemoryAllowedActionRules.evaluate(
                true, "VM", fields, null, null, NOW);
        assertTrue(decision.getAllowedActions().contains("apply"));
        assertTrue(decision.getAllowedActions().contains("clearOverride"));
        assertFalse(decision.getAllowedActions().contains("pause"));
        assertFalse(decision.getAllowedActions().contains("drain"));
        assertFalse(decision.getAllowedActions().contains("resume"));
        assertFalse(decision.getPreflightRequiredActions().contains(MemoryBackendPreparationRules.ACTION));
        assertFalse(decision.getPreflightRequiredActions().contains(MemoryZramPoolPreparationRules.ACTION));
    }

    @Test
    public void hostLifecycleActionsComeOnlyFromSharedFreshControlProjection() {
        MemoryStateVO freshActive = controlledState(false);
        MemoryAllowedActionRules.Decision active = MemoryAllowedActionRules.evaluate(
                true, "Host", writableFields(), freshActive, view(freshActive, false, false), NOW);
        assertTrue(active.getAllowedActions().containsAll(Arrays.asList("pause", "drain")));
        assertFalse(active.getAllowedActions().contains("resume"));

        MemoryStateVO freshPaused = controlledState(true);
        MemoryAllowedActionRules.Decision paused = MemoryAllowedActionRules.evaluate(
                true, "Host", writableFields(), freshPaused, view(freshPaused, true, false), NOW);
        assertTrue(paused.getAllowedActions().contains("resume"));
        assertFalse(paused.getAllowedActions().contains("pause"));
    }

    @Test
    public void unknownActiveTaskOrMissingControlNeverAdvertisesResume() {
        MemoryStateVO unknown = controlledState(true);
        unknown.setStatus("Unknown");
        MemoryAllowedActionRules.Decision unknownDecision = MemoryAllowedActionRules.evaluate(
                true, "Host", writableFields(), unknown, view(unknown, true, false), NOW);
        assertFalse(unknownDecision.getAllowedActions().contains("resume"));

        MemoryStateVO activeTask = controlledState(true);
        activeTask.setActiveTaskUuid("task-1");
        MemoryAllowedActionRules.Decision activeDecision = MemoryAllowedActionRules.evaluate(
                true, "Host", writableFields(), activeTask, view(activeTask, true, false), NOW);
        assertFalse(activeDecision.getAllowedActions().contains("resume"));

        MemoryStateVO noControl = controlledState(true);
        noControl.setControlOperationUuid(null);
        MemoryAllowedActionRules.Decision noControlDecision = MemoryAllowedActionRules.evaluate(
                true, "Host", writableFields(), noControl, view(noControl, true, false), NOW);
        assertFalse(noControlDecision.getAllowedActions().contains("resume"));
        assertPreflightIsAllowedWorkflowSubset(noControlDecision);
    }

    private static MemoryStateVO state(long sampleTime, String capabilities) {
        MemoryStateVO state = new MemoryStateVO();
        state.setLastSampleTime(sampleTime);
        state.setCapabilities(capabilities);
        return state;
    }

    private static MemoryStateVO controlledState(boolean paused) {
        MemoryStateVO state = state(900, "{\"supported\":true,\"zram\":true,\"writeback\":true}");
        state.setHostUuid("host-1");
        state.setStatus("Succeeded");
        state.setDesiredRevision(4);
        state.setAppliedRevision(4L);
        state.setControlOperationUuid("control-1");
        state.setState("{\"managed\":true,\"bootId\":\"boot-1\",\"poolGeneration\":\"pool-1\","
                + "\"lastConfirmedOperationUuid\":\"control-1\",\"lastConfirmedAppliedRevision\":4,"
                + "\"lifecycle\":{\"paused\":" + paused + ",\"activeState\":\""
                + (paused ? "paused" : "active") + "\"},\"host_zram\":{\"pool_generation\":\"pool-1\","
                + "\"quality\":\"native_host_device\",\"device\":\"/dev/zram0\","
                + "\"observed_at\":\"1970-01-01T00:00:00.900Z\"}}");
        return state;
    }

    private static MemoryStateInventory view(MemoryStateVO state, boolean canResume, boolean canReconcile) {
        MemoryStateInventory view = state.toInventory();
        MemoryStatePresentation.populate(view, NOW, canResume, canReconcile);
        return view;
    }

    private static Map<String, MemoryFieldCapability> writableFields() {
        Map<String, MemoryFieldCapability> fields = new LinkedHashMap<>();
        fields.put("zram.enabled", new MemoryFieldCapability(true, true, "boolean", null, null));
        return fields;
    }

    private static void assertPreflightIsAllowedWorkflowSubset(MemoryAllowedActionRules.Decision decision) {
        assertTrue("Every preflight action must have an allowed workflow entry",
                decision.getAllowedActions().containsAll(decision.getPreflightRequiredActions()));
    }
}
