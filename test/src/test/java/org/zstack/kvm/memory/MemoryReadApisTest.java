package org.zstack.kvm.memory;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class MemoryReadApisTest {

    @Test public void allowedActionsFollowTheOperatorControlAndTheObservedHost() {
        java.util.Map<String, Object> observed = new java.util.LinkedHashMap<>();
        observed.put("control", "active");
        observed.put("ksm", Collections.singletonMap("enabled", true));
        observed.put("zram", Collections.singletonMap("poolGeneration", "pool"));
        assertEquals("Active", MemoryRepository.featureState(observed, MemoryRepository.ZramExpectation.NONE));
        assertEquals(java.util.Arrays.asList("query", "pause", "drain"), MemoryRepository.allowedActions("active", MemoryRepository.SUCCEEDED, observed));
        assertEquals(java.util.Arrays.asList("query", "resume"), MemoryRepository.allowedActions("paused", MemoryRepository.SUCCEEDED, observed));
        assertEquals(java.util.Arrays.asList("query", "resume"), MemoryRepository.allowedActions("drained", MemoryRepository.SUCCEEDED, observed));
        observed.put("control", "drained");
        assertEquals("Drained", MemoryRepository.featureState(observed, MemoryRepository.ZramExpectation.NONE));
        assertEquals("Unknown", MemoryRepository.featureState(Collections.emptyMap(), MemoryRepository.ZramExpectation.NONE));
        assertEquals(Collections.singletonList("query"), MemoryRepository.allowedActions("active", MemoryRepository.SUCCEEDED, Collections.emptyMap()));
        // A Host that gave up, or is still observed drained, can be retried with resume.
        assertEquals(java.util.Arrays.asList("query", "resume"),
                MemoryRepository.allowedActions("active", MemoryRepository.FAILED, Collections.emptyMap()));
        assertEquals(java.util.Arrays.asList("query", "resume"),
                MemoryRepository.allowedActions("active", MemoryRepository.SUCCEEDED, Collections.singletonMap("control", "drained")));
    }

    @Test public void supportedCapabilitiesBecomeHostTagsAndUnknownOnesKeepThem() {
        Map<String, Object> capabilities = new LinkedHashMap<>();
        capabilities.put("supported", true); capabilities.put("ksm", true); capabilities.put("ksmZeroPages", false);
        capabilities.put("zram", true); capabilities.put("writeback", null); capabilities.put("service", true);
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("capabilities", capabilities);
        assertEquals(new TreeSet<>(Arrays.asList("ksm", "zram")), MemoryOptimizationManager.supportedCapabilities(state));
        capabilities.put("supported", false); capabilities.put("ksm", false); capabilities.put("zram", false);
        assertEquals("an unsupported Host has no capability tag",
                Collections.emptySet(), MemoryOptimizationManager.supportedCapabilities(state));
        capabilities.put("supported", null);
        assertNull("capabilities the Agent could not discover leave the tags as they are",
                MemoryOptimizationManager.supportedCapabilities(state));
        assertNull(MemoryOptimizationManager.supportedCapabilities(Collections.emptyMap()));
    }

    @Test public void aHostReportedUnsupportedCannotRunZram() {
        Map<String, Object> capabilities = new LinkedHashMap<>();
        capabilities.put("supported", false); capabilities.put("reasonCode", "UNSUPPORTED_OS_RELEASE");
        Map<String, Object> observed = new LinkedHashMap<>();
        observed.put("capabilities", capabilities);
        assertFalse("a Host that reports itself unsupported, without a ZRAM verdict, cannot run ZRAM",
                MemoryRepository.zramSupported(observed));
        assertEquals("Unsupported", MemoryRepository.featureState(observed, MemoryRepository.ZramExpectation.NONE));
    }

    @Test public void aSampleTheAgentCouldNotCompleteIsAFailedObservation() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("inventoryComplete", true);
        state.put("zram", Collections.singletonMap("serviceState", "active"));
        assertNull(MemoryOptimizationManager.incompleteSample(state));
        state.put("zram", Collections.singletonMap("serviceState", "unknown"));
        assertEquals("a service state systemd could not report is a failed observation",
                "SERVICE_STATE_UNKNOWN", MemoryOptimizationManager.incompleteSample(state));
        state.put("inventoryComplete", false);
        assertEquals("VMs libvirt could not list are a failed observation",
                "VM_INVENTORY_INCOMPLETE", MemoryOptimizationManager.incompleteSample(state));
        state.put("inventoryComplete", true);
        state.put("zram", Collections.singletonMap("serviceState", "absent"));
        assertNull("a Host without the managed service is not a failure", MemoryOptimizationManager.incompleteSample(state));
    }
}
