package org.zstack.kvm.memory;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class MemorySwapDiskInventoryTest {
    private static final String HOST = "093d46206e694835b4a218449eb1bc7c";
    private static final String ID = "candidate:" + String.join("", Collections.nCopies(64, "a"));

    private static Map<String, Object> candidate(String state, String... reasons) {
        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("id", ID);
        candidate.put("device", "/dev/nvme0n1");
        candidate.put("byIdPath", "/dev/disk/by-id/nvme-X_S674");
        candidate.put("serial", "S674");
        candidate.put("wwn", "");
        candidate.put("capacityBytes", 256060514304L);
        candidate.put("state", state);
        candidate.put("reasons", Arrays.asList(reasons));
        return candidate;
    }

    private static Map<String, Object> agentState(String status, Map<String, Object> disk, Map<String, Object>... candidates) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("hostUuid", HOST);
        state.put("status", status);
        state.put("swapDisk", disk);
        state.put("otherSwaps", Collections.singletonList("/dev/zram0"));
        state.put("preparationReasons", Collections.singletonList("OTHER_SWAP_ACTIVE"));
        state.put("candidates", Arrays.asList(candidates));
        return state;
    }

    @Test
    public void projectsCandidatesWithTheirApiStates() {
        MemorySwapDiskInventory inventory = MemorySwapDiskInventory.fromAgentState(HOST, HOST, agentState("NOT_CONFIGURED", null,
                candidate("AVAILABLE"), candidate("NEEDS_OVERWRITE_CONFIRMATION", "EXISTING_FILESYSTEM_OR_PARTITION_SIGNATURE"),
                candidate("INELIGIBLE", "ZRAM_WRITEBACK_BACKEND")));
        assertEquals("NotConfigured", inventory.getStatus());
        assertNull(inventory.getSwapDisk());
        assertEquals(Collections.singletonList("/dev/zram0"), inventory.getOtherSwaps());
        assertEquals(Collections.singletonList("OTHER_SWAP_ACTIVE"), inventory.getPreparationReasons());
        MemorySwapDiskDeviceInventory first = inventory.getCandidates().get(0);
        assertEquals(ID, first.getId());
        assertEquals(Long.valueOf(256060514304L), first.getSize());
        assertNull("a blank WWN is unknown", first.getWwn());
        assertEquals(Arrays.asList("Available", "NeedsOverwriteConfirmation", "Ineligible"),
                Arrays.asList(first.getState(), inventory.getCandidates().get(1).getState(), inventory.getCandidates().get(2).getState()));
        assertEquals(Collections.singletonList("ZRAM_WRITEBACK_BACKEND"), inventory.getCandidates().get(2).getReasons());
    }

    @Test
    public void theSwapDiskIsActiveOnlyWithoutReasons() {
        Map<String, Object> disk = candidate(null);
        disk.remove("id"); disk.remove("state");
        disk.put("uuid", "2f1d7c1e-6a1e-4c55-9d1a-1b5d2a7e9c01");
        disk.put("usedBytes", 1L << 30);
        MemorySwapDiskDeviceInventory active = MemorySwapDiskInventory.fromAgentState(HOST, HOST, agentState("ACTIVE", disk)).getSwapDisk();
        assertEquals("Active", active.getState());
        assertEquals("2f1d7c1e-6a1e-4c55-9d1a-1b5d2a7e9c01", active.getSwapUuid());
        assertEquals(Long.valueOf(1L << 30), active.getUsedSize());
        disk.put("reasons", Collections.singletonList("SWAP_DISK_NOT_FOUND"));
        disk.remove("usedBytes");
        MemorySwapDiskInventory fault = MemorySwapDiskInventory.fromAgentState(HOST, HOST, agentState("FAULT", disk));
        assertEquals("Fault", fault.getStatus());
        assertEquals("Fault", fault.getSwapDisk().getState());
        assertNull(fault.getSwapDisk().getUsedSize());
    }

    @Test
    public void missingCandidateListIsUnknownButAnExplicitEmptyListIsValid() {
        Map<String, Object> missing = agentState("NOT_CONFIGURED", null);
        missing.remove("candidates");
        Map<String, Object> nullList = agentState("NOT_CONFIGURED", null);
        nullList.put("candidates", null);
        for (Map<String, Object> state : Arrays.asList(missing, nullList)) {
            try {
                MemorySwapDiskInventory.fromAgentState(HOST, HOST, state);
                fail("accepted an unknown candidate list");
            } catch (IllegalArgumentException expected) {
                assertEquals("SWAP_DISK_CANDIDATES_UNKNOWN", expected.getMessage());
            }
        }

        Map<String, Object> empty = agentState("NOT_CONFIGURED", null);
        empty.put("candidates", Collections.emptyList());
        assertTrue(MemorySwapDiskInventory.fromAgentState(HOST, HOST, empty).getCandidates().isEmpty());
    }

    @Test
    public void anotherHostOrAnUnknownStateIsRefused() {
        for (Runnable bad : Arrays.<Runnable>asList(
                () -> MemorySwapDiskInventory.fromAgentState(HOST, "f" + HOST.substring(1), agentState("ACTIVE", null)),
                () -> MemorySwapDiskInventory.fromAgentState(HOST, HOST, agentState("BROKEN", null)),
                () -> MemorySwapDiskInventory.fromAgentState(HOST, HOST, agentState("NOT_CONFIGURED", null, candidate("RESERVED"))))) {
            try {
                bad.run();
                fail("accepted a malformed inventory");
            } catch (IllegalArgumentException expected) {
                assertNotNull(expected.getMessage());
            }
        }
    }

    @Test
    public void anAgentThatDidNotDecideCannotPrepare() {
        Map<String, Object> state = agentState("NOT_CONFIGURED", null);
        state.remove("preparationReasons");
        assertEquals(Collections.singletonList("PREPARATION_STATE_UNKNOWN"),
                MemorySwapDiskInventory.fromAgentState(HOST, HOST, state).getPreparationReasons());
    }

    private static MemoryAgentResponse response(boolean success, String status, String reasonCode) {
        MemoryAgentResponse response = new MemoryAgentResponse();
        response.setSuccess(success);
        response.status = status;
        response.reasonCode = reasonCode;
        return response;
    }

    @Test
    public void aRefusalNamesTheAgentReasonAndAFailedStepIsAFailure() {
        assertEquals("success", MemoryOptimizationManager.swapDiskOutcome(response(true, "Succeeded", null)));
        assertEquals("Failed: AGENT_UNREACHABLE", MemoryOptimizationManager.swapDiskOutcome(null));
        MemoryAgentResponse refused = response(false, "Failed", "INSUFFICIENT_MEMORY_FOR_SWAPOFF");
        refused.details = Collections.singletonMap("requiredBytes", 3);
        assertEquals("MEMORY_BACKEND_PREPARATION_INVALID: INSUFFICIENT_MEMORY_FOR_SWAPOFF {requiredBytes=3}",
                MemoryOptimizationManager.swapDiskOutcome(refused));
        assertEquals("Failed: SWAP_DISK_COMMAND_FAILED",
                MemoryOptimizationManager.swapDiskOutcome(response(false, "Failed", "SWAP_DISK_COMMAND_FAILED")));
        assertEquals("MEMORY_HOST_OPERATION_FAILED", MemoryClusterOperationReply.hostError("Failed: SWAP_DISK_COMMAND_FAILED").getCode());
        assertEquals("MEMORY_BACKEND_PREPARATION_INVALID",
                MemoryClusterOperationReply.hostError(MemoryOptimizationManager.swapDiskOutcome(refused)).getCode());
    }
}
