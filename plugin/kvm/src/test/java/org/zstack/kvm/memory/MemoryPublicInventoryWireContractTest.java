package org.zstack.kvm.memory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Pins canonical public names and only those temporary aliases used by r12 UI consumers. */
public class MemoryPublicInventoryWireContractTest {
    private final Gson gson = new Gson();

    @Test
    public void vmProjectionUsesCanonicalNamesAndBoundedUiAliases() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("observed_at", "2026-10-10T00:00:00Z");
        report.put("host_boot_id", "boot");
        report.put("schema_version", 2);
        report.put("ownership_semantics", "source_page_memcg");
        report.put("inventory", Collections.singletonMap("pool_generation", "123"));
        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("uuid", "vm");
        identity.put("instance_generation", "instance-1");
        identity.put("host_boot_id", "boot");
        identity.put("pool_generation", "123");
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("ram_original_bytes", 10L);
        metrics.put("ram_payload_bytes", 12L);
        metrics.put("backend_committed_original_bytes", 3L);
        metrics.put("ram_logical_minus_payload_bytes", -2L);
        Map<String, Object> vm = new LinkedHashMap<>();
        vm.put("quality", "fresh");
        vm.put("observed_at", report.get("observed_at"));
        vm.put("identity", identity);
        vm.put("metrics", metrics);
        report.put("vms", Collections.singletonMap("vm", vm));

        long sampledAt = java.time.Instant.parse("2026-10-10T00:00:00Z").toEpochMilli();
        JsonObject output = gson.toJsonTree(MemoryObservationRules.vm("host", "vm", report,
                sampledAt + 1000L, 60_000L)).getAsJsonObject();
        assertTrue(output.has("observedAt"));
        assertTrue(output.has("observed_at")); // r12 VM-list compatibility
        assertEquals(output.get("observedAt"), output.get("observed_at"));
        assertTrue(output.getAsJsonObject("identity").has("instanceGeneration"));
        assertTrue(output.getAsJsonObject("identity").has("instance_generation")); // r12 VM-list compatibility
        assertEquals(output.getAsJsonObject("identity").get("instanceGeneration"),
                output.getAsJsonObject("identity").get("instance_generation"));
        JsonObject projectedMetrics = output.getAsJsonObject("metrics");
        assertEquals(-2L, projectedMetrics.get("ramLogicalMinusPayloadBytes").getAsLong());
        assertEquals(10L, projectedMetrics.get("ramOriginalBytes").getAsLong());
        assertEquals(10L, projectedMetrics.get("ram_original_bytes").getAsLong()); // r12 VM list/detail
        assertTrue(projectedMetrics.has("ramPayloadBytes"));
        assertTrue(projectedMetrics.has("ram_payload_bytes"));
        assertTrue(projectedMetrics.has("backendCommittedOriginalBytes"));
        assertTrue(projectedMetrics.has("backend_committed_original_bytes"));
        assertEquals(projectedMetrics.get("ramPayloadBytes"), projectedMetrics.get("ram_payload_bytes"));
        assertEquals(projectedMetrics.get("ramOriginalBytes"), projectedMetrics.get("ram_original_bytes"));
        assertEquals(projectedMetrics.get("backendCommittedOriginalBytes"),
                projectedMetrics.get("backend_committed_original_bytes"));
        assertFalse(projectedMetrics.has("ram_logical_minus_payload_bytes"));
        assertFalse(projectedMetrics.has("ratio_state"));
        assertEquals(new HashSet<>(Arrays.asList("observed_at", "instance_generation", "ram_payload_bytes",
                        "ram_original_bytes", "backend_committed_original_bytes")),
                serializedAliases(output));
        assertEquals(new HashSet<>(Arrays.asList("observed_at", "instance_generation", "ram_payload_bytes",
                        "ram_original_bytes", "backend_committed_original_bytes", "request_id", "updated_at")),
                declaredAliasFields());

        MemoryVmAccountingInventory vmInventory = MemoryObservationRules.vm("host", "vm", report,
                sampledAt + 1000L, 60_000L);
        vmInventory.setObservedAt(null);
        vmInventory.getIdentity().setInstanceGeneration(null);
        vmInventory.getMetrics().setRamPayloadBytes(null);
        vmInventory.getMetrics().setRamOriginalBytes(null);
        vmInventory.getMetrics().setBackendCommittedOriginalBytes(null);
        JsonObject cleared = gson.toJsonTree(vmInventory).getAsJsonObject();
        assertFalse(cleared.has("observedAt"));
        assertFalse(cleared.has("observed_at"));
        assertFalse(cleared.getAsJsonObject("identity").has("instanceGeneration"));
        assertFalse(cleared.getAsJsonObject("identity").has("instance_generation"));
        JsonObject clearedMetrics = cleared.getAsJsonObject("metrics");
        assertFalse(clearedMetrics.has("ramPayloadBytes"));
        assertFalse(clearedMetrics.has("ram_payload_bytes"));
        assertFalse(clearedMetrics.has("ramOriginalBytes"));
        assertFalse(clearedMetrics.has("ram_original_bytes"));
        assertFalse(clearedMetrics.has("backendCommittedOriginalBytes"));
        assertFalse(clearedMetrics.has("backend_committed_original_bytes"));
    }

    @Test
    public void maintenanceKeepsOnlyTheTwoObservedUiAliases() {
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("boot_id", "boot");
        target.put("canonical_device", "/dev/vdb");
        target.put("capacity_bytes", 1024L);
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("schema", 1);
        source.put("request_id", "request");
        source.put("candidate_id", "candidate");
        source.put("request_fingerprint", "fingerprint");
        source.put("stage", "APPLIED");
        source.put("started_at", "2026-10-10T00:00:00Z");
        source.put("updated_at", "2026-10-10T00:00:01Z");
        source.put("host_boot_id", "boot");
        source.put("pool_generation", "123");
        source.put("target_identity", target);
        source.put("reset_attempted", true);
        source.put("backend_resource_uuid", "resource");

        JsonObject output = gson.toJsonTree(MemoryWritebackMaintenanceInventory.fromMap(source)).getAsJsonObject();
        assertTrue(output.has("requestId"));
        assertTrue(output.has("request_id")); // r12 configuration page
        assertEquals(output.get("requestId"), output.get("request_id"));
        assertTrue(output.has("updatedAt"));
        assertTrue(output.has("updated_at")); // r12 configuration page
        assertEquals(output.get("updatedAt"), output.get("updated_at"));
        assertTrue(output.has("candidateId"));
        assertFalse(output.has("candidate_id"));
        assertTrue(output.has("targetIdentity"));
        assertTrue(output.getAsJsonObject("targetIdentity").has("capacityBytes"));
        assertFalse(output.has("target_identity"));
        assertEquals(new HashSet<>(Arrays.asList("request_id", "updated_at")), serializedAliases(output));

        MemoryWritebackMaintenanceInventory inventory = MemoryWritebackMaintenanceInventory.fromMap(source);
        inventory.setRequestId(null);
        inventory.setUpdatedAt(null);
        JsonObject cleared = gson.toJsonTree(inventory).getAsJsonObject();
        assertFalse(cleared.has("requestId"));
        assertFalse(cleared.has("request_id"));
        assertFalse(cleared.has("updatedAt"));
        assertFalse(cleared.has("updated_at"));
    }

    private Set<String> serializedAliases(JsonObject object) {
        Set<String> aliases = new HashSet<>();
        collectSerializedAliases(object, aliases);
        return aliases;
    }

    private Set<String> declaredAliasFields() {
        Set<String> fields = new HashSet<>();
        for (Class<?> type : Arrays.asList(MemoryVmAccountingInventory.class, MemoryVmIdentityInventory.class,
                MemoryVmAccountingMetricsInventory.class, MemoryWritebackMaintenanceInventory.class)) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getName().contains("_")) {
                    fields.add(field.getName());
                }
            }
        }
        return fields;
    }

    private void collectSerializedAliases(JsonObject object, Set<String> keys) {
        for (Map.Entry<String, com.google.gson.JsonElement> entry : object.entrySet()) {
            if (entry.getValue().isJsonObject()) {
                collectSerializedAliases(entry.getValue().getAsJsonObject(), keys);
            } else if (entry.getValue().isJsonArray()) {
                for (com.google.gson.JsonElement item : entry.getValue().getAsJsonArray()) {
                    if (item.isJsonObject()) {
                        collectSerializedAliases(item.getAsJsonObject(), keys);
                    }
                }
            }
            if (entry.getKey().contains("_")) {
                keys.add(entry.getKey());
            }
        }
    }

    @Test
    public void serviceConcurrencyKeysAreProjectedToNamedFields() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("operation_id", "op"); row.put("vm_uuid", ""); row.put("kind", "writeback");
        row.put("status", "running"); row.put("started_at", "2026-10-10T00:00:00Z");
        Map<String, Object> raw = new LinkedHashMap<>();
        raw.put("entries", Collections.singletonList(row)); raw.put("total", 1); raw.put("nextPage", null);
        Map<String, Object> cq = new LinkedHashMap<>(); cq.put("C", 2); cq.put("Q", 3); raw.put("cq", cq);
        Map<String, Object> budget = new LinkedHashMap<>(); budget.put("actual_allocated_bytes", 7L);
        budget.put("reserved_bytes", 5L); budget.put("budget_bytes", 10L); budget.put("records", 2);
        budget.put("protected_records", 1); raw.put("record_budget", budget);

        JsonObject output = gson.toJsonTree(MemoryHostOperationsInventory.fromAgentState("host", "host", raw))
                .getAsJsonObject();
        assertEquals(2, output.getAsJsonObject("concurrency").get("activeOperations").getAsInt());
        assertEquals(3, output.getAsJsonObject("concurrency").get("isolatedTimeoutOperations").getAsInt());
        assertFalse(output.has("cq"));
        assertTrue(output.getAsJsonObject("recordBudget").has("actualAllocatedBytes"));
        assertEquals("", output.getAsJsonArray("entries").get(0).getAsJsonObject().get("vmUuid").getAsString());
    }
}
