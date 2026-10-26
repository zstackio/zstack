package org.zstack.kvm.memory;

import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.zstack.header.rest.RestRequest;
import java.util.*;
import static org.junit.Assert.*;

public class MemoryReadApisTest {
    public static class TypedNumberFixture {
        public Long whole;
        public Double fraction;
    }

    private static void assertExactUnsignedInteger(String expected, Object value) {
        assertTrue(value instanceof Number);
        assertFalse(value instanceof Double);
        assertFalse(value instanceof Float);
        assertEquals(new java.math.BigInteger(expected),
                new java.math.BigDecimal(value.toString()).toBigIntegerExact());
    }
    @Test public void prometheusManagerCallbackDoesNotRequireAgentHostUuidEnvelope() {
        MemoryAgentResponse response = new MemoryAgentResponse();
        response.setSuccess(true);
        response.state = new LinkedHashMap<>();
        response.state.put("observed_at", java.time.Instant.now().toString());
        Map<String,Object> vm = new LinkedHashMap<>();
        vm.put("quality", "source_memcg_inventory");
        vm.put("metrics", Collections.singletonMap("ram_original_bytes", 4096L));
        response.state.put("vms", Collections.singletonMap("vm", vm));
        // callVmMetrics is the Prometheus provider path: its response has no Agent envelope hostUuid.
        assertNull(response.hostUuid);
        MemoryVmAccountingInventory inventory = MemoryOptimizationManager.vmObservation("host", "vm", response);
        assertEquals("host", inventory.getHostUuid());
        assertEquals(Long.valueOf(4096L), inventory.getMetrics().getRam_original_bytes());
    }

    @Test public void vmQueryKeepsUnknownAndNeverLeaksOtherVmRows() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("quality", "atomic_inventory_identity_checked");
        report.put("observed_at", java.time.Instant.now().toString());
        report.put("vms", Collections.singletonMap("other-vm", Collections.singletonMap("metrics", 99)));
        MemoryVmAccountingInventory unavailable = MemoryObservationRules.vm("host", "vm1", report, System.currentTimeMillis(), 90000L);
        assertEquals("unavailable", unavailable.getQuality());
        assertNull(unavailable.getMetrics());
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("quality", "source_memcg_inventory");
        record.put("metrics", Collections.singletonMap("ram_original_bytes", 4096));
        report.put("vms", Collections.singletonMap("vm1", record));
        Map<String,Object> metrics=new LinkedHashMap<>(); metrics.put("ram_original_bytes",4096L); record.put("metrics",metrics);
        Map<String,Object> identity=new LinkedHashMap<>(); identity.put("uuid","vm1"); record.put("identity",identity);
        Map<String, Object> present = new LinkedHashMap<>(); present.put("vm1",record); report.put("vms",present);
        MemoryVmAccountingInventory typed = MemoryObservationRules.vm("host", "vm1", report, System.currentTimeMillis(), 90000L);
        assertEquals(Long.valueOf(4096L), typed.getMetrics().getRam_original_bytes());
        assertEquals("source_memcg_inventory", typed.getQuality());
    }

    @Test public void expiredVmSamplesAreNotRenewedByQueryOrFilledWithZero() {
        Map<String, Object> report = new LinkedHashMap<>();
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("quality", "source_memcg_inventory");
        record.put("metrics", Collections.singletonMap("ram_original_bytes", 4096));
        report.put("vms", Collections.singletonMap("vm", record));
        report.put("observed_at", "2026-10-07T00:00:00Z");
        long observed = java.time.Instant.parse((String) report.get("observed_at")).toEpochMilli();
        assertNotNull(MemoryObservationRules.vm("host", "vm", report, observed + 89999, 90000).getMetrics());
        MemoryVmAccountingInventory expired = MemoryObservationRules.vm("host", "vm", report, observed + 90000, 90000);
        assertEquals("stale", expired.getQuality()); assertNull(expired.getMetrics());
        assertEquals(report.get("observed_at"), expired.getObserved_at());
        assertNull(MemoryObservationRules.vm("host", "vm", report, observed - 5001, 90000).getMetrics());
        report.remove("observed_at");
        assertEquals("unavailable", MemoryObservationRules.vm("host", "vm", report, observed, 90000).getQuality());
    }

    @Test public void pausedStateExposesResumeFenceAndAllowedAction() {
        MemoryStateVO state = new MemoryStateVO();
        state.setHostUuid("host"); state.setStatus("Succeeded");
        state.setControlOperationUuid("pause-operation"); state.setLastSampleTime(System.currentTimeMillis());
        state.setState("{\"lifecycle\":{\"paused\":true,\"activeState\":\"paused\"}}");
        MemoryStateInventory inventory = state.toInventory();
        assertEquals("pause-operation", inventory.getControlOperationUuid());
        // A plain state row cannot establish which durable operation owns the
        // fence; the API service adds resume only after checking the task row.
        assertEquals(Collections.singletonList("query"), inventory.getAllowedActions());
    }
}
