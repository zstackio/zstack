package org.zstack.kvm.memory;

import com.google.gson.Gson;
import org.junit.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MemoryWritebackBackendInventoryTest {
    private static final String HOST = "093d46206e694835b4a218449eb1bc7c";

    @Test
    public void convertsCapturedAgentCandidateAndDropsUnknownFields() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("hostUuid", HOST);
        state.put("bootId", "8c6ce488-e264-4c1d-bcd9-763196d10a3e");
        state.put("observedAt", "2026-10-08T14:04:20.720910Z");
        state.put("status", "AVAILABLE");
        state.put("reasons", new ArrayList<String>());

        Map<String, Object> stable = new LinkedHashMap<>();
        stable.put("bootId", state.get("bootId"));
        stable.put("device", "/dev/vdb");
        stable.put("byIdPath", "/dev/disk/by-id/virtio-e4e9ca6be5425a879187");
        stable.put("majorMinor", "252:16");
        stable.put("serial", "e4e9ca6be5425a879187");
        stable.put("wwn", "");
        stable.put("capacityBytes", 21474836480L);
        stable.put("transport", "");
        stable.put("sysfsPath", "/sys/devices/pci0000:00/0000:00:0c.0/virtio6/block/vdb");
        stable.put("sysfsInode", "79610");
        stable.put("rdev", 64528L);
        stable.put("nodeInode", 1043854454L);
        stable.put("unknownAgentDiagnostic", "must-not-be-forwarded");

        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("identity", "e9a8d7cb8b6dc595a1c589841e83eb2d10760690a1c9dad3e2c4e970695f253b");
        candidate.put("fingerprint", candidate.get("identity"));
        candidate.put("resourceUuid", "6266ba305b5ed487b5f22cefee13f466");
        candidate.put("type", "block");
        candidate.put("path", stable.get("byIdPath"));
        candidate.put("device", "/dev/vdb");
        candidate.put("byIdPath", stable.get("byIdPath"));
        candidate.put("capacityBytes", 21474836480L);
        candidate.put("hostBootId", state.get("bootId"));
        candidate.put("eligible", true);
        candidate.put("writebackReady", true);
        candidate.put("state", "PREPARED");
        candidate.put("reasons", new ArrayList<String>());
        candidate.put("transport", "");
        candidate.put("stableIdentity", stable);
        candidate.put("active", false);
        candidate.put("unknownInternalField", "must-not-be-forwarded");
        state.put("candidates", Arrays.asList(candidate));
        state.put("canPrepare", false);
        state.put("preparationReasons", Arrays.asList("MEMORY_SERVICE_NOT_STOPPED"));
        state.put("canPrepareZramPool", false);
        state.put("zramPoolPreparationReasons", Arrays.asList("DRAIN_PROOF_UNAVAILABLE"));
        state.put("zramPoolPreparationQuality", "Ineligible");
        state.put("zramPoolPreparationObservedAt", 1791468260938L);
        Map<String, Object> targetIdentity = new LinkedHashMap<>();
        targetIdentity.put("boot_id", state.get("bootId"));
        targetIdentity.put("canonical_device", "/dev/vdb");
        targetIdentity.put("rdev", 64528L);
        targetIdentity.put("node_inode", 1043854454L);
        targetIdentity.put("sysfs_inode", 79610L);
        targetIdentity.put("capacity_bytes", 21474836480L);
        targetIdentity.put("direct_io", false);
        targetIdentity.put("stack_sha256", "866d2053446ba0b001fdf9b46dbe373f418c09be30fcd465c5a006884600f75c");
        targetIdentity.put("agentPrivateField", "must-not-be-forwarded");
        Map<String, Object> maintenance = new LinkedHashMap<>();
        maintenance.put("schema", 1);
        maintenance.put("request_id", "288097e635094a56afe759757cc5397f");
        maintenance.put("candidate_id", "candidate:724d4582adec92a9bd70b2da9d9c2965fbc1506e71987b7201e2d782d12de065");
        maintenance.put("request_fingerprint", "13114029099baf472d0f3942440f800967e7a904896695de16d38196d5b80af8");
        maintenance.put("stage", "APPLIED");
        maintenance.put("started_at", "2026-10-08T09:22:55.375876961Z");
        maintenance.put("updated_at", "2026-10-08T11:51:44.459781937Z");
        maintenance.put("host_boot_id", state.get("bootId"));
        maintenance.put("pool_generation", "af013f2fc83c7845def03466c8612396eb8604a1ddccd6ff4283f0e0317f2333");
        maintenance.put("target_identity", targetIdentity);
        maintenance.put("reset_attempted", true);
        maintenance.put("archive", "/var/lib/zram-efficiency-writeback-20261007/backend-maintenance/288097e635094a56afe759757cc5397f/pre-reset-archive");
        maintenance.put("backend_resource_uuid", "6266ba305b5ed487b5f22cefee13f466");
        maintenance.put("newServiceInternalField", "must-not-be-forwarded");
        state.put("maintenance", maintenance);
        state.put("unknownInternalField", "must-not-be-forwarded");

        MemoryWritebackBackendInventory result = MemoryWritebackBackendInventory.fromAgentState(HOST, HOST, state);
        MemoryWritebackBackendCandidateInventory projected = result.getCandidates().get(0);
        assertEquals(HOST, result.getHostUuid());
        assertEquals(Long.valueOf(21474836480L), projected.getCapacityBytes());
        assertEquals("6266ba305b5ed487b5f22cefee13f466", projected.getResourceUuid());
        assertEquals("8c6ce488-e264-4c1d-bcd9-763196d10a3e", projected.getHostBootId());
        assertEquals(Long.valueOf(21474836480L), projected.getStableIdentity().getCapacityBytes());
        assertEquals(Long.valueOf(1043854454L), projected.getStableIdentity().getNodeInode());
        assertEquals("APPLIED", result.getMaintenance().getStage());
        assertEquals("288097e635094a56afe759757cc5397f", result.getMaintenance().getRequest_id());
        assertEquals("2026-10-08T11:51:44.459781937Z", result.getMaintenance().getUpdated_at());
        assertEquals(Long.valueOf(21474836480L), result.getMaintenance().getTarget_identity().getCapacity_bytes());
        String json = new Gson().toJson(result);
        assertTrue(json.contains("\"resourceUuid\":\"6266ba305b5ed487b5f22cefee13f466\""));
        assertFalse(json.contains("unknownInternalField"));
        assertFalse(json.contains("unknownAgentDiagnostic"));
        assertFalse(json.contains("qualified"));
        assertFalse(json.contains("agentPrivateField"));
        assertFalse(json.contains("newServiceInternalField"));
    }

    @Test
    public void preservesExactLongCapacityAndRejectsRoundedDouble() {
        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("capacityBytes", new BigInteger("9007199254740993"));
        assertEquals(Long.valueOf(9007199254740993L),
                MemoryWritebackBackendCandidateInventory.fromMap(candidate).getCapacityBytes());
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
    public void preservesMissingNullAndEmptyCandidateSemantics() {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("hostUuid", HOST);
        MemoryWritebackBackendInventory missing = MemoryWritebackBackendInventory.fromAgentState(HOST, HOST, state);
        assertNull(missing.getCandidates());
        assertEquals(Boolean.FALSE, missing.getCandidatesFieldPresent());
        state.put("candidates", null);
        MemoryWritebackBackendInventory explicitNull = MemoryWritebackBackendInventory.fromAgentState(HOST, HOST, state);
        assertNull(explicitNull.getCandidates());
        assertEquals(Boolean.TRUE, explicitNull.getCandidatesFieldPresent());
        state.put("candidates", new ArrayList<Map<String, Object>>());
        MemoryWritebackBackendInventory empty = MemoryWritebackBackendInventory.fromAgentState(HOST, HOST, state);
        assertTrue(empty.getCandidates().isEmpty());
        assertEquals(Boolean.TRUE, empty.getCandidatesFieldPresent());
    }

    @Test
    public void rejectsMismatchedHostIdentityBeforeProjectingState() {
        Map<String, Object> state = new LinkedHashMap<>();
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
