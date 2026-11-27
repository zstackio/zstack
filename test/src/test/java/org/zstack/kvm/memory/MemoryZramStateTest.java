package org.zstack.kvm.memory;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/** ZSTAC-92147: the Host's ZRAM state on its own, apart from KSM and from the configuration delivery. */
public class MemoryZramStateTest {
    private static Map<String, Object> desired(Boolean zram) {
        Map<String, Object> desired = new LinkedHashMap<>();
        desired.put("zram", Collections.singletonMap("enabled", zram));
        desired.put("ksm", Collections.singletonMap("enabled", true));
        return desired;
    }

    /** A Host reclaiming with ZRAM: service active, pool built, reclaim mode, reclaim on, KSM merging. */
    private static Map<String, Object> reclaiming() {
        Map<String, Object> zram = new LinkedHashMap<>();
        zram.put("serviceState", "active");
        zram.put("poolGeneration", "pool");
        zram.put("mode", "reclaim");
        zram.put("efficiencyEnabled", true);
        Map<String, Object> observed = new LinkedHashMap<>();
        observed.put("control", "active");
        observed.put("capabilities", new LinkedHashMap<>(Collections.singletonMap("zram", true)));
        observed.put("ksm", Collections.singletonMap("enabled", true));
        observed.put("zram", zram);
        return observed;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> zram(Map<String, Object> observed) { return (Map<String, Object>) observed.get("zram"); }

    private static String state(Map<String, Object> observed, Map<String, Object> desired, String status) {
        return MemoryRepository.zramState(observed, desired, status);
    }

    @Test public void aHostReclaimingWithZramIsActive() {
        assertEquals("Active", state(reclaiming(), desired(true), "Applied"));
    }

    @Test public void anUnmanagedSwitchUsesTheObservedZramState() {
        assertEquals("Active", state(reclaiming(), desired(null), "Applied"));
        assertEquals("Unknown", state(Collections.emptyMap(), desired(null), "Applied"));
        Map<String, Object> disk = desired(null);
        disk.put("reclaim", Collections.singletonMap("backend", "swap"));
        disk.put("zram", Collections.emptyMap());
        assertEquals("Disabled", state(reclaiming(), disk, "Applied"));
    }

    @Test public void onlyKsmWorkingIsNotZramActive() {
        // The case of ZSTAC-89836: KSM keeps the Host's feature state Active while ZRAM is off.
        Map<String, Object> observed = reclaiming();
        zram(observed).put("efficiencyEnabled", false);
        assertEquals("Active", MemoryRepository.featureState(observed, MemoryRepository.ZramExpectation.NONE));
        assertEquals("Disabled", state(observed, desired(false), "Applied"));
    }

    @Test public void aHostThatCannotRunZramIsUnsupportedWhateverItsConfiguration() {
        Map<String, Object> observed = reclaiming();
        ((Map<String, Object>) observed.get("capabilities")).put("zram", false);
        assertEquals("Unsupported", state(observed, desired(true), "Applied"));
        assertEquals("Unsupported", state(observed, desired(false), "Applied"));
        Map<String, Object> whole = new LinkedHashMap<>(Collections.singletonMap("capabilities",
                Collections.singletonMap("supported", false)));
        assertEquals("a Host unsupported as a whole", "Unsupported", state(whole, desired(true), "Applied"));
    }

    @Test public void aFailedDeliveryOfAWantedZramIsFailed() {
        assertEquals("Failed", state(reclaiming(), desired(true), "Failed"));
        assertEquals("a failure does not concern a ZRAM that is off", "Disabled", state(reclaiming(), desired(false), "Failed"));
    }

    @Test public void withoutAnObservationOrAConfigurationTheStateIsUnknown() {
        assertEquals("Unknown", state(Collections.emptyMap(), desired(true), "Applied"));
        assertEquals("Unknown", state(reclaiming(), null, "Converging"));
        Map<String, Object> stopped = reclaiming();
        zram(stopped).put("serviceState", "inactive");
        assertEquals("a wanted ZRAM whose service is not running", "Unknown", state(stopped, desired(true), "Applied"));
    }

    @Test public void drainedAndPausedFollowTheObservedControl() {
        Map<String, Object> observed = reclaiming();
        observed.put("control", "drained");
        zram(observed).put("serviceState", "inactive");
        assertEquals("Drained", state(observed, desired(true), "Applied"));
        observed = reclaiming();
        observed.put("control", "paused");
        assertEquals("Paused", state(observed, desired(true), "Applied"));
    }

    @Test public void aWantedZramWaitingForItsPoolIsPreparing() {
        Map<String, Object> observed = reclaiming();
        zram(observed).put("poolGeneration", null);
        assertEquals("Preparing", state(observed, desired(true), "Converging"));
    }

    @Test public void aPoolThatDoesNotReclaimIsInactive() {
        Map<String, Object> observed = reclaiming();
        zram(observed).put("mode", "observe");
        assertEquals("Inactive", state(observed, desired(true), "Applied"));
        observed = reclaiming();
        zram(observed).put("efficiencyEnabled", false);
        assertEquals("Inactive", state(observed, desired(true), "Applied"));
    }
}
