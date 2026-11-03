package org.zstack.kvm.memory;

import org.junit.Test;

import java.math.BigInteger;
import java.util.*;

import static org.junit.Assert.*;

public class MemoryWritebackBackendInventoryTest {
    private static final String HOST = "093d46206e694835b4a218449eb1bc7c";

    /** An Agent inventory as the writeback-backends call returns it. */
    private static Map<String, Object> agentState() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("hostUuid", HOST);
        state.put("bootId", "8c6ce488-e264-4c1d-bcd9-763196d10a3e");
        state.put("observedAt", "2026-10-08T14:04:20.720910Z");
        state.put("status", "AVAILABLE");
        state.put("reasons", Collections.emptyList());
        state.put("poolGeneration", "none");
        state.put("canPrepare", true);
        state.put("preparationReasons", Collections.emptyList());
        state.put("maintenance", Collections.singletonMap("stage", "APPLIED"));
        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("identity", "candidate:" + String.join("", Collections.nCopies(64, "e")));
        candidate.put("fingerprint", String.join("", Collections.nCopies(64, "e")));
        candidate.put("resourceUuid", null);
        candidate.put("type", "block");
        candidate.put("path", "/dev/disk/by-id/virtio-e4e9ca6be5425a879187");
        candidate.put("device", "/dev/vdb");
        candidate.put("byIdPath", "/dev/disk/by-id/virtio-e4e9ca6be5425a879187");
        candidate.put("capacityBytes", 21474836480L);
        candidate.put("hostBootId", state.get("bootId"));
        candidate.put("state", "NEEDS_OFFLINE_PREPARATION");
        candidate.put("reasons", Collections.singletonList("NEEDS_OFFLINE_PREPARATION"));
        candidate.put("transport", "virtio");
        Map<String, Object> stable = new LinkedHashMap<>();
        stable.put("serial", "e4e9ca6be5425a879187");
        stable.put("wwn", "");
        stable.put("majorMinor", "252:16");
        candidate.put("stableIdentity", stable);
        state.put("candidates", Collections.singletonList(candidate));
        return state;
    }

    @Test
    public void projectsTheAgentInventoryOntoTheApiFields() {
        MemoryWritebackBackendInventory inventory = MemoryWritebackBackendInventory.fromAgentState(HOST, HOST, agentState());
        assertEquals(HOST, inventory.getHostUuid());
        assertEquals("Available", inventory.getStatus());
        assertEquals(Long.valueOf(1791468260720L), inventory.getObservedAt());
        assertTrue("an empty list means the Host can prepare now", inventory.getPreparationReasons().isEmpty());
        MemoryWritebackBackendCandidateInventory candidate = inventory.getCandidates().get(0);
        assertEquals("candidate:" + String.join("", Collections.nCopies(64, "e")), candidate.getId());
        assertEquals("/dev/disk/by-id/virtio-e4e9ca6be5425a879187", candidate.getPath());
        assertEquals("/dev/vdb", candidate.getDevice());
        assertEquals(Long.valueOf(21474836480L), candidate.getSize());
        assertEquals("e4e9ca6be5425a879187", candidate.getSerial());
        assertNull("a blank WWN is not known", candidate.getWwn());
        assertEquals("virtio", candidate.getTransport());
        assertEquals("Available", candidate.getState());
    }

    @Test
    public void eachAgentStateHasItsApiName() {
        String[][] states = {{"NEEDS_OFFLINE_PREPARATION", "Available"}, {"PREPARED", "Prepared"}, {"ACTIVE", "Active"},
                {"INELIGIBLE", "Ineligible"}, {"DISCONNECTED", "Disconnected"}, {"IDENTITY_MISMATCH", "IdentityMismatch"}};
        for (String[] state : states) {
            Map<String, Object> candidate = new LinkedHashMap<>();
            candidate.put("state", state[0]);
            assertEquals(state[1], MemoryWritebackBackendCandidateInventory.fromMap(candidate).getState());
        }
        String[][] statuses = {{"AVAILABLE", "Available"}, {"PARTIAL", "Available"}, {"DISCONNECTED", "Disconnected"},
                {"UNSUPPORTED", "Unsupported"}};
        for (String[] status : statuses) {
            Map<String, Object> state = agentState();
            state.put("status", status[0]);
            assertEquals(status[1], MemoryWritebackBackendInventory.fromAgentState(HOST, HOST, state).getStatus());
        }
        Map<String, Object> unknown = new LinkedHashMap<>();
        unknown.put("state", "PREPARING");
        try {
            MemoryWritebackBackendCandidateInventory.fromMap(unknown);
            fail("an unknown state must not be passed on");
        } catch (IllegalArgumentException expected) {
            assertEquals("WRITEBACK_CANDIDATE_STATE_UNKNOWN", expected.getMessage());
        }
    }

    @Test
    public void anAgentThatDidNotDecideCannotPrepareAndHasNoCandidates() {
        Map<String, Object> state = agentState();
        state.remove("preparationReasons");
        state.remove("candidates");
        MemoryWritebackBackendInventory inventory = MemoryWritebackBackendInventory.fromAgentState(HOST, HOST, state);
        assertEquals(Collections.singletonList("PREPARATION_STATE_UNKNOWN"), inventory.getPreparationReasons());
        assertTrue(inventory.getCandidates().isEmpty());
    }

    @Test
    public void aMalformedObservationTimeIsAMalformedInventory() {
        Map<String, Object> state = agentState();
        state.put("observedAt", "yesterday");
        try {
            MemoryWritebackBackendInventory.fromAgentState(HOST, HOST, state);
            fail("a malformed time must not be passed on");
        } catch (IllegalArgumentException expected) {
            assertEquals("WRITEBACK_OBSERVED_AT_MALFORMED", expected.getMessage());
        }
    }

    @Test
    public void preservesExactLongCapacityAndRejectsRoundedDouble() {
        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("state", "PREPARED");
        candidate.put("capacityBytes", new BigInteger("9007199254740993"));
        assertEquals(Long.valueOf(9007199254740993L), MemoryWritebackBackendCandidateInventory.fromMap(candidate).getSize());
        candidate.put("capacityBytes", 9007199254740992d);
        try {
            MemoryWritebackBackendCandidateInventory.fromMap(candidate);
            fail("rounded double must not be treated as an exact byte count");
        } catch (IllegalArgumentException expected) {
            assertEquals("WRITEBACK_INTEGER_PRECISION_UNSAFE", expected.getMessage());
        }
    }

    @Test
    public void integerBoundariesRejectNegativeAndUnsafeFloatingPointButPreserveZeroAndNull() {
        assertEquals(Long.valueOf(0), MemoryWritebackBackendInventory.exactLong(0));
        assertNull(MemoryWritebackBackendInventory.exactLong(null));
        assertEquals(Long.valueOf(16777215), MemoryWritebackBackendInventory.exactLong(16777215f));
        assertEquals(Long.valueOf(9007199254740991L),
                MemoryWritebackBackendInventory.exactLong(9007199254740991d));
        for (Object unsafe : Arrays.<Object>asList(16777216f, 9007199254740992d, -1L)) {
            try {
                MemoryWritebackBackendInventory.exactLong(unsafe);
                fail("unsafe or negative integer accepted: " + unsafe);
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage().startsWith("WRITEBACK_INTEGER_"));
            }
        }
    }

    @Test
    public void nullableStringListsRejectNullElements() {
        assertNull(MemoryWritebackBackendInventory.strings(null));
        assertEquals(Arrays.asList("A", "B"), MemoryWritebackBackendInventory.strings(Arrays.asList("A", "B")));
        try {
            MemoryWritebackBackendInventory.strings(Arrays.asList("A", null));
            fail("null list item must not be silently serialized");
        } catch (IllegalArgumentException expected) {
            assertEquals("WRITEBACK_STRING_LIST_MALFORMED", expected.getMessage());
        }
    }

    @Test
    public void rejectsMismatchedHostIdentityBeforeProjectingState() {
        Map<String, Object> state = agentState();
        state.put("hostUuid", "another-host");
        try {
            MemoryWritebackBackendInventory.fromAgentState(HOST, HOST, state);
            fail("must reject state from another host");
        } catch (IllegalArgumentException expected) {
            assertEquals("WRITEBACK_INVENTORY_HOST_IDENTITY_MISMATCH", expected.getMessage());
        }
        state.put("hostUuid", HOST);
        try {
            MemoryWritebackBackendInventory.fromAgentState(HOST, "foreign-host", state);
            fail("outer Agent host mismatch must be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals("WRITEBACK_INVENTORY_HOST_IDENTITY_MISMATCH", expected.getMessage());
        }
    }
}
