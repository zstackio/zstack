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
}
