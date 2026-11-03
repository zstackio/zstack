package org.zstack.kvm.memory;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class MemoryBackendPreparationRulesTest {
    private static final String CANDIDATE = "candidate:" + String.join("", Collections.nCopies(64, "a"));

    /** An Agent inventory in which the candidate is a blank disk that can be prepared now. */
    private static Map<String, Object> inventory(Map<String, Object> candidate) {
        Map<String, Object> inventory = new LinkedHashMap<>();
        inventory.put("bootId", "1e73a145-2662-4911-9c4f-b75a7d91a88e");
        inventory.put("poolGeneration", "none");
        inventory.put("preparationReasons", new ArrayList<>());
        inventory.put("candidates", Collections.singletonList(candidate));
        return inventory;
    }

    private static Map<String, Object> candidate() {
        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("identity", CANDIDATE);
        candidate.put("state", "NEEDS_OFFLINE_PREPARATION");
        candidate.put("capacityBytes", 20L << 30);
        return candidate;
    }

    @Test public void onlyADiscoveredCandidateIdIsAccepted() {
        MemoryBackendPreparationRules.validate(CANDIDATE);
        for (String invalid : Arrays.asList(null, "/dev/vdb", "candidate:abc")) {
            try { MemoryBackendPreparationRules.validate(invalid); fail("accepted " + invalid); }
            catch (MemoryOperationException expected) { assertEquals("MEMORY_BACKEND_PREPARATION_INVALID", expected.getCode()); }
        }
    }

    @Test public void aBlankDiskTheHostCanPrepareNowIsAccepted() {
        assertNull(MemoryBackendPreparationRules.refusal(inventory(candidate()), CANDIDATE));
    }

    @Test public void theHostsCurrentInventoryDecidesWhetherThePreparationStarts() {
        assertEquals("CANDIDATE_NOT_FOUND", MemoryBackendPreparationRules.refusal(inventory(candidate()), "candidate:other"));

        Map<String, Object> prepared = candidate(); prepared.put("state", "PREPARED");
        assertEquals("CANDIDATE_NOT_AVAILABLE", MemoryBackendPreparationRules.refusal(inventory(prepared), CANDIDATE));

        Map<String, Object> busy = inventory(candidate());
        busy.put("preparationReasons", Arrays.asList("MEMORY_SERVICE_NOT_STOPPED", "POOL_GENERATION_UNKNOWN"));
        assertEquals("the Host's first reason is reported", "MEMORY_SERVICE_NOT_STOPPED",
                MemoryBackendPreparationRules.refusal(busy, CANDIDATE));

        Map<String, Object> undecided = inventory(candidate()); undecided.remove("preparationReasons");
        assertEquals("PREPARATION_STATE_UNKNOWN", MemoryBackendPreparationRules.refusal(undecided, CANDIDATE));

        Map<String, Object> unknownPool = inventory(candidate()); unknownPool.put("poolGeneration", null);
        assertEquals("HOST_IDENTITY_UNKNOWN", MemoryBackendPreparationRules.refusal(unknownPool, CANDIDATE));
    }

    @Test public void onlyAWholeNumberOfPagesCanBackWriteback() {
        for (Object capacity : Arrays.<Object>asList(4096L, (20L << 30) + 1, 10737418240.5, null, "10737418240")) {
            Map<String, Object> candidate = candidate(); candidate.put("capacityBytes", capacity);
            assertEquals("capacity " + capacity, "CANDIDATE_CAPACITY_INVALID",
                    MemoryBackendPreparationRules.refusal(inventory(candidate), CANDIDATE));
        }
        Map<String, Object> exactDouble = candidate(); exactDouble.put("capacityBytes", (double) (20L << 30));
        assertNull("JSON numbers arrive as doubles", MemoryBackendPreparationRules.refusal(inventory(exactDouble), CANDIDATE));
    }
}
