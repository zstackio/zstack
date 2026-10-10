package org.zstack.kvm.memory;

import org.junit.Test;
import java.lang.reflect.InvocationTargetException;
import static org.junit.Assert.*;

public class MemoryTaskRulesTest {
    private Object call(String name, Class<?>[] types, Object... values) throws Exception {
        Class<?> rules;
        try {
            rules = Class.forName("org.zstack.kvm.memory.MemoryTaskRules");
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Memory task safety contract is not implemented", e);
        }
        try {
            return rules.getMethod(name, types).invoke(null, values);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e.getCause();
            }
            throw e;
        }
    }

    @Test public void expiryExemptionCannotSmuggleConfiguration() throws Exception {
        Class<?>[] signature = {String.class, String.class};
        // Safety payload validation remains separate from the commercial gate.
        assertEquals(true, call("isSafetyAction", signature, "pause", "{}"));
        assertEquals(true, call("isSafetyAction", signature, "drain", "{}"));
        assertEquals(true, call("isSafetyAction", signature, "resume", "{}"));
        assertEquals(true, call("isSafetyAction", signature, "reconcile", "{}"));
        assertEquals(false, call("isSafetyAction", signature, "apply", "{}"));
        assertEquals(false, call("isSafetyAction", signature, "clearOverride", "{}"));
        assertEquals(false, call("isSafetyAction", signature, "pause", "{\"zram\":{\"enabled\":true}}"));
        assertEquals(false, call("isSafetyAction", signature, "drain", "null"));

        assertEquals(true, call("requiresLicenseForConfiguration", signature, "apply", "{}"));
        assertEquals(true, call("requiresLicenseForConfiguration", signature, "clearOverride", "{}"));
        assertEquals(true, call("requiresLicenseForConfiguration", signature, "pause", "{}"));
        assertEquals(true, call("requiresLicenseForConfiguration", signature, "drain", "{}"));
        assertEquals(true, call("requiresLicenseForConfiguration", signature, "resume", "{}"));
        assertEquals(true, call("requiresLicenseForConfiguration", signature, "apply", "{\"participation\":\"deny\"}"));
        assertEquals(false, call("requiresLicenseForConfiguration", signature, "reconcile", "{}"));
        assertEquals(true, call("requiresLicenseForConfiguration", signature, "reconcile", "{\"ksm\":{}}"));
    }

    @Test public void unknownBlocksConflictingWorkAndOnlyQueuedCanCancel() throws Exception {
        Class<?>[] signature = {String.class};
        assertEquals(true, call("blocksHost", signature, "Unknown"));
        assertEquals(true, call("blocksHost", signature, "Draining"));
        assertEquals(true, call("blocksHost", signature, "Blocked"));
        assertEquals(false, call("blocksHost", signature, "Succeeded"));
        assertEquals(false, call("canCancel", signature, "Unknown"));
        assertEquals(false, call("canCancel", signature, "Applying"));
        assertEquals(true, call("canCancel", signature, "Queued"));
    }

    @Test public void timeoutIsNeverSuccessAndTerminalCannotBeReplayed() throws Exception {
        Class<?>[] signature = {String.class, String.class};
        assertEquals(true, call("canTransition", signature, "Applying", "Unknown"));
        assertEquals(false, call("canTransition", signature, "Succeeded", "Applying"));
        assertEquals(false, call("canTransition", signature, "Unknown", "Applying"));
        assertEquals(false, call("canTransition", signature, "Queued", "Succeeded"));
    }

    @Test public void resumeAcceptsPeriodicLifecycleSnapshotButNotDrainOrHold() {
        assertTrue(MemoryRepository.isConfirmedPausedState("{\"phase\":\"PAUSED\"}"));
        assertTrue(MemoryRepository.isConfirmedPausedState(
                "{\"lifecycle\":{\"paused\":true,\"activeState\":\"paused\"}}"));
        assertFalse(MemoryRepository.isConfirmedPausedState(
                "{\"lifecycle\":{\"paused\":true,\"activeState\":\"migration_hold\"}}"));
        assertFalse(MemoryRepository.isConfirmedPausedState("{\"phase\":\"DRAINING\"}"));
        assertFalse(MemoryRepository.isConfirmedPausedState("{}"));
    }

    @Test public void drainRecoveryRequiresFreshServiceOwnedMaintenanceProof() {
        String proof = "{\"maintenanceProof\":{\"maintenanceReady\":true,"
                + "\"maintenanceArchived\":true,\"poolOwnedByService\":true,"
                + "\"drainOperationUuid\":\"drain-1\",\"oldPoolGeneration\":\"old\","
                + "\"newPoolGeneration\":\"new\",\"activeOperations\":0,\"executorExited\":true},"
                + "\"poolGeneration\":\"new\"}";
        assertTrue(MemoryRepository.isMaintenanceRecoveryState(proof, "drain-1"));
        assertFalse(MemoryRepository.isMaintenanceRecoveryState(proof, "drain-2"));
        assertFalse(MemoryRepository.isMaintenanceRecoveryState(proof.replace("\"new\"", "\"old\""), "drain-1"));
        assertFalse(MemoryRepository.isMaintenanceRecoveryState(proof.replace("\"activeOperations\":0", "\"activeOperations\":1"), "drain-1"));
    }

    @Test public void drainRecoveryAllowsOnlyExplicitReadyForInitializationAfterReset() {
        String proof = "{\"maintenanceProof\":{\"maintenanceReady\":true,"
                + "\"maintenanceArchived\":true,\"drainOperationUuid\":\"drain-1\","
                + "\"activeOperations\":0,\"readyForInitialization\":true,"
                + "\"originalDeviceInactive\":true,\"oldPoolOwnershipAbsent\":true,\"executorExited\":true}}";
        assertTrue(MemoryRepository.isMaintenanceRecoveryState(proof, "drain-1"));
        assertFalse(MemoryRepository.isMaintenanceRecoveryState(proof, "drain-2"));
        assertFalse(MemoryRepository.isMaintenanceRecoveryState(
                proof.replace("\"oldPoolOwnershipAbsent\":true", "\"oldPoolOwnershipAbsent\":false"), "drain-1"));
        assertFalse(MemoryRepository.isMaintenanceRecoveryState(
                proof.replace("\"originalDeviceInactive\":true", "\"originalDeviceInactive\":false"), "drain-1"));
        assertFalse(MemoryRepository.isMaintenanceRecoveryState(
                proof.replace("\"readyForInitialization\":true", "\"readyForInitialization\":false"), "drain-1"));
    }

    @Test public void maintenanceProofIsCarriedToApplyCommandWithoutInventingOne() {
        Object proof = MemoryOptimizationManager.maintenanceProofForCommand(
                "{\"maintenanceProof\":{\"maintenanceReady\":true,\"drainOperationUuid\":\"d\","
                        + "\"oldPoolGeneration\":\"a\",\"newPoolGeneration\":\"b\"}}");
        assertTrue(proof instanceof java.util.Map);
        assertNull(MemoryOptimizationManager.maintenanceProofForCommand("{\"managed\":true}"));
        assertNull(MemoryOptimizationManager.maintenanceProofForCommand("not-json"));
    }

    @Test public void legacyKsmIsBlockedOnlyForKnownManagedOwnership() {
        MemoryLegacyKsmGuard.validate(false, "none", "true");
        try {
            MemoryLegacyKsmGuard.validate(true, "true", "true");
            fail("managed host KSM must use the memory policy API");
        } catch (org.zstack.core.config.GlobalConfigException expected) {
            assertTrue(expected.getMessage().contains("MEMORY_CONTROLLER_CONFLICT"));
        }
        try {
            MemoryLegacyKsmGuard.validateDelete(true);
            fail("managed host KSM delete must use the memory policy API");
        } catch (org.zstack.core.config.GlobalConfigException expected) {
            assertTrue(expected.getMessage().contains("MEMORY_CONTROLLER_CONFLICT"));
        }
        MemoryLegacyKsmGuard.validateDelete(false);
    }
}
