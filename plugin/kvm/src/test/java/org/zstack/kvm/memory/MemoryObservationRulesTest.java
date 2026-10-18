package org.zstack.kvm.memory;

import org.junit.Test;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.time.OffsetDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MemoryObservationRulesTest {
    @Test public void nativeServiceEventsRequireExplicitFailedOrActiveNotScanningOrUnknown() {
        long now = System.currentTimeMillis();
        MemoryPolicyConfig policy = MemoryPolicyRules.decode("{\"ksm\":{\"enabled\":true}}");
        MemoryAgentResponse response = serviceObservation(now);
        Map<String, Object> ksm = new LinkedHashMap<>();
        response.state.put("actual", Collections.singletonMap("ksm", ksm));
        ksm.put("enabled", null); ksm.put("scanning", false);
        ksm.put("serviceState", "failed"); ksm.put("tunedServiceState", "activating");
        assertEquals(1, MemoryObservationRules.serviceEvents("host", response, policy, now).size());
        assertEquals("Failed", MemoryObservationRules.serviceEvents("host", response, policy, now).get(0).getStatus());
        ksm.put("serviceState", "active");
        assertEquals("Recovered", MemoryObservationRules.serviceEvents("host", response, policy, now).get(0).getStatus());
        for (Object state : new Object[]{null, "inactive", "activating", "deactivating", "unknown"}) {
            ksm.put("serviceState", state);
            assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, now).size());
        }
        ksm.put("tunedServiceState", "failed");
        assertEquals("ksmtuned", MemoryObservationRules.serviceEvents("host", response, policy, now).get(0).getService());
        response.setSuccess(false); response.status = "Unknown";
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, now).size());
        response.reasonCode = "LOCAL_SERVICE_UNAVAILABLE";
        assertEquals(1, MemoryObservationRules.serviceEvents("host", response, policy, now).size());
        org.junit.Assert.assertFalse(MemoryObservationRules.isKsmNativePartial(response, "host", now));
        for (Boolean enabled : new Boolean[]{false, null}) {
            policy.ksm.enabled = enabled;
            assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, now).size());
        }
    }

    @Test public void serviceEventsRejectStaleForeignUnidentifiedAndPausedSamples() {
        long now = System.currentTimeMillis();
        MemoryPolicyConfig policy = MemoryPolicyRules.decode("{\"ksm\":{\"enabled\":true}}");
        MemoryAgentResponse response = serviceObservation(now);
        response.state.put("actual", Collections.singletonMap("ksm", Collections.singletonMap("serviceState", "failed")));
        response.sampleTime = now - MemoryOptimizationGlobalConfig.displayTtlMillis();
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, now).size());
        response.sampleTime = now + 1;
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, now).size());
        response.sampleTime = now; response.hostUuid = "other-host";
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, now).size());
        response.hostUuid = "host"; response.state.remove("bootId");
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, now).size());
        response.state.put("bootId", "boot"); response.state.put("maintenance_blocks_activation", true);
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, now).size());
        response.state.put("maintenance_blocks_activation", false);
        response.state.put("lifecycle", Collections.singletonMap("paused", true));
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, now).size());
    }

    @Test public void serviceEventsRejectConflictingEnvelopeAndStateBootIds() {
        long now = System.currentTimeMillis();
        MemoryPolicyConfig policy = MemoryPolicyRules.decode("{\"ksm\":{\"enabled\":true}}");
        MemoryAgentResponse response = serviceObservation(now);
        response.hostUuid = "host";
        response.state.put("actual", Collections.singletonMap("ksm",
                Collections.singletonMap("serviceState", "failed")));
        response.bootId = "other-boot";
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, now).size());
        response.bootId = "boot";
        assertEquals(1, MemoryObservationRules.serviceEvents("host", response, policy, now).size());
    }

    @Test public void writebackFailureDoesNotTreatUnknownFenceOrHistoricalBaselineAsIoFailure() {
        long now = OffsetDateTime.parse("2026-10-13T00:00:00Z").toInstant().toEpochMilli();
        MemoryAgentResponse response = writebackObservation(now);
        MemoryPolicyConfig policy = MemoryPolicyRules.decode("{\"zram\":{\"enabled\":true},\"writeback\":{\"enabled\":true}}");
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("state", "DEGRADED"); health.put("reason", "NATIVE_BACKEND_IO_ERROR_BACKOFF");
        health.put("new_io_permitted_by_backend_health", false); response.state.put("backend_health", health);
        assertEquals("Failed", MemoryObservationRules.serviceEvents("host", response, policy, now).get(0).getStatus());
        for (String reason : new String[]{"UNKNOWN_OPERATION_NO_REPLAY", "NATIVE_ERROR_BASELINE_RECORDED", "NO_BACKEND"}) {
            health.put("reason", reason);
            assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, now).size());
        }
        health.put("reason", "NATIVE_BACKEND_IO_ERROR_BACKOFF");
        response.state.put("observed_at", "2020-01-01T00:00:00Z");
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, now).size());
        response.state.put("observed_at", "2026-10-13T00:00:00Z"); response.state.put("resource_authorized", false);
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, now).size());
    }

    @Test public void writebackRecoveryRequiresSuccessfulIoAfterBackoffNotIdleOrElapsedTimeAlone() {
        long now = OffsetDateTime.parse("2026-10-13T00:00:00Z").toInstant().toEpochMilli();
        MemoryAgentResponse response = writebackObservation(now);
        MemoryPolicyConfig policy = MemoryPolicyRules.decode("{\"zram\":{\"enabled\":true},\"writeback\":{\"enabled\":true}}");
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("state", "READY"); health.put("reason", "BACKOFF_ELAPSED_BOUNDED_IO_PERMITTED");
        health.put("new_io_permitted_by_backend_health", true); health.put("backoff_until", "2026-10-12T23:59:50Z");
        response.state.put("backend_health", health);
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, now).size());
        Map<String, Object> schedule = new LinkedHashMap<>(); schedule.put("pool_generation", "pool-1");
        schedule.put("last_completed_id", "operation-1"); response.state.put("writeback_schedule", schedule);
        Map<String, Object> pool = new LinkedHashMap<>(); pool.put("pool_generation", "pool-1");
        pool.put("quality", "native_host_device"); response.state.put("host_zram", pool);
        Map<String, Object> before = new LinkedHashMap<>(); before.put("quality", "native_host_device");
        before.put("observed_at", "2026-10-12T23:59:51Z");
        before.put("committed_writeback_pages_total", 10L); before.put("read_errors", 1L); before.put("write_errors", 2L);
        Map<String, Object> after = new LinkedHashMap<>(before);
        Map<String, Object> returned = new LinkedHashMap<>(); returned.put("operation_id", "operation-1");
        returned.put("returned_at", "2026-10-12T23:59:52Z"); returned.put("kernel_called", true);
        returned.put("before", before); returned.put("after", after); response.state.put("last_writeback_return", returned);
        assertEquals("idle/no progress is not recovery", 0, MemoryObservationRules.serviceEvents("host", response, policy, now).size());
        after.put("committed_writeback_pages_total", 11L);
        assertEquals("Recovered", MemoryObservationRules.serviceEvents("host", response, policy, now).get(0).getStatus());
        after.put("write_errors", 3L);
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, now).size());
        after.put("write_errors", 2L); returned.put("error", "failed");
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, now).size());
        returned.remove("error"); returned.put("kernel_called", false);
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, now).size());
        returned.put("kernel_called", true); health.put("backoff_until", "2026-10-12T23:59:59Z");
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, now).size());
        health.put("backoff_until", "2026-10-12T23:59:50Z"); schedule.put("pool_generation", "old-pool");
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, now).size());
    }

    private static MemoryAgentResponse serviceObservation(long now) {
        MemoryAgentResponse response = new MemoryAgentResponse(); response.setSuccess(true); response.status = "Succeeded";
        response.sampleTime = now; response.state = new LinkedHashMap<>(); response.state.put("bootId", "boot");
        return response;
    }

    private static MemoryAgentResponse writebackObservation(long now) {
        MemoryAgentResponse response = serviceObservation(now); response.state.put("resource_authorized", true);
        response.state.put("observed_at", "2026-10-13T00:00:00Z"); return response;
    }

    @Test public void capturedVmSampleConvertsAsFreshAndPreservesProcIdentityAndMetrics() {
        String observedAt = "2026-10-08T00:39:24.983155465+08:00";
        long sampleMillis = OffsetDateTime.parse(observedAt).toInstant().toEpochMilli();
        Map<String, Object> report = report(observedAt);
        report.put("schema_version", 2);
        report.put("host_boot_id", "8c6ce488-e264-4c1d-bcd9-763196d10a3e");
        report.put("device", "/dev/zram0");
        report.put("quality", "partial_vm_attribution");
        report.put("ownership_semantics",
                "source_page_memcg_at_successful_commit; KSM is charge ownership, not fair shared cost");
        Map<String, Object> inventory = new LinkedHashMap<>();
        inventory.put("pool_generation", 1051626769722038414L);
        inventory.put("sequence", 1177499L);
        report.put("inventory", inventory);

        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("uuid", "03d98313faa24670a0944b10da8d1ac2");
        identity.put("host_boot_id", "8c6ce488-e264-4c1d-bcd9-763196d10a3e");
        identity.put("instance_generation", "96a20bcfedbd7fd6759c775ebb82333555d51a623b68da55d4c5c1ed114e1da7");
        identity.put("pid", 3279904);
        identity.put("start_time", "116424049");
        identity.put("cgroup_path", "/sys/fs/cgroup/machine.slice/machine-qemu\\x2d7\\x2d03d98313faa24670a0944b10da8d1ac2.scope/libvirt");
        identity.put("cgroup_inode", "239821");

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("compressed_pages", 20137L);
        metrics.put("incompressible_pages", 3005L);
        metrics.put("zero_pages", 2760L);
        metrics.put("nonzero_same_pages", 162L);
        metrics.put("backend_committed_pages", 0L);
        metrics.put("ram_payload_bytes", 38014641L);
        metrics.put("ram_original_bytes", 106758144L);
        metrics.put("backend_committed_original_bytes", 0L);
        metrics.put("ram_logical_minus_payload_bytes", 68743503L);
        metrics.put("ram_logical_to_payload_ratio", 2.8083428171793074d);
        metrics.put("ratio_state", "finite");

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("quality", "partial_unattributed_present");
        entry.put("observed_at", observedAt);
        entry.put("identity", identity);
        entry.put("metrics", metrics);
        report.put("vms", Collections.singletonMap("03d98313faa24670a0944b10da8d1ac2", entry));

        MemoryVmAccountingInventory result = MemoryObservationRules.vm(
                "093d46206e694835b4a218449eb1bc7c", "03d98313faa24670a0944b10da8d1ac2",
                report, sampleMillis + 1000L, 90000L);
        assertEquals("partial_unattributed_present", result.getQuality());
        assertNull(result.getReason());
        assertEquals("1051626769722038414", result.getPoolGeneration());
        assertEquals("1177499", result.getSequence());
        assertEquals("116424049", result.getIdentity().getStart_time());
        assertEquals("239821", result.getIdentity().getCgroup_inode());
        assertEquals("96a20bcfedbd7fd6759c775ebb82333555d51a623b68da55d4c5c1ed114e1da7",
                result.getIdentity().getInstance_generation());
        assertEquals(Long.valueOf(106758144L), result.getMetrics().getRam_original_bytes());
        assertEquals(Long.valueOf(38014641L), result.getMetrics().getRam_payload_bytes());
    }

    @Test public void prometheusRecordsKeepPerVmTimeAndExactIdentityLabels() {
        Map<String, Object> report = report("2026-10-07T00:00:00Z");
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("quality", "source_memcg_inventory");
        record.put("observed_at", "2026-10-06T22:51:06Z");
        Map<String, Object> identity = new LinkedHashMap<>();
        identity.put("pool_generation", "18446744073709551615");
        identity.put("host_boot_id", "boot");
        record.put("identity", identity);
        record.put("metrics", Collections.emptyMap());
        report.put("vms", Collections.singletonMap("vm", record));
        MemoryVmAccountingInventory result = MemoryObservationRules.vm("host", "vm", report,
                1791327067000L, 90000L);
        assertEquals("source_memcg_inventory", result.getQuality());
        assertEquals("18446744073709551615", result.getPoolGeneration());
        assertEquals("boot", result.getHost_boot_id());
        assertEquals(record.get("observed_at"), result.getObserved_at());
        record.put("observed_at", "invalid");
        assertEquals("SAMPLE_TIME_INVALID", MemoryObservationRules.vm("host", "vm", report,
                1791327067000L, 90000L).getReason());
    }

    @Test public void acceptsRfc3339OffsetsAndFractionalSeconds() {
        Map<String, Object> report = report("2026-10-07T06:51:06.000612311+08:00");
        MemoryVmAccountingInventory result = MemoryObservationRules.vm("host", "vm", report,
                1791327067000L, 90000L);
        assertEquals("Fresh", result.getQuality());
        assertEquals(Long.valueOf(90000L), result.getDisplayTtlMillis());
    }

    @Test public void invalidSampleTimeIsUnavailableNotFresh() {
        MemoryVmAccountingInventory result = MemoryObservationRules.vm("host", "vm", report("not-a-time"),
                1791327067000L, 90000L);
        assertEquals("SAMPLE_TIME_INVALID", result.getReason());
        assertEquals("unavailable", result.getQuality());
    }

    @Test public void uint64InventoryIdentityIsExposedAsString() {
        Map<String, Object> report = report("2026-10-07T06:51:06Z");
        Map<String, Object> inventory = new LinkedHashMap<>();
        inventory.put("pool_generation", 15704573862423239L);
        inventory.put("sequence", 477777L);
        report.put("inventory", inventory);
        MemoryVmAccountingInventory result = MemoryObservationRules.vm("host", "vm", report,
                1791327067000L, 90000L);
        assertEquals("15704573862423239", result.getPoolGeneration());
        assertEquals("477777", result.getSequence());
    }

    @Test public void unsafeDoubleIdentityIsNotPublishedAsFakePrecision() {
        Map<String, Object> report = report("2026-10-07T06:51:06Z");
        Map<String, Object> inventory = new LinkedHashMap<>();
        inventory.put("pool_generation", 1.5704573862423239E19d);
        report.put("inventory", inventory);
        MemoryVmAccountingInventory result = MemoryObservationRules.vm("host", "vm", report,
                1791327067000L, 90000L);
        assertEquals("unavailable", result.getQuality());
        assertEquals("IDENTITY_TOKEN_UNSAFE", result.getReason());
        org.junit.Assert.assertNull(result.getMetrics());
    }

    @Test public void safeDoubleIntegerUsesDecimalIntegerText() {
        Map<String, Object> report = report("2026-10-07T06:51:06Z");
        Map<String, Object> inventory = new LinkedHashMap<>();
        inventory.put("pool_generation", 477777.0d);
        report.put("inventory", inventory);
        MemoryVmAccountingInventory result = MemoryObservationRules.vm("host", "vm", report,
                1791327067000L, 90000L);
        assertEquals("477777", result.getPoolGeneration());
    }

    @Test public void metricProjectionKeepsZeroDistinctFromAbsentAndRejectsFractionalCounters() {
        Map<String,Object> metrics=new LinkedHashMap<>(); metrics.put("zero_pages",0); metrics.put("ram_original_bytes",4096L);
        metrics.put("ram_logical_to_payload_ratio",2.5); metrics.put("ratio_state","finite");
        Map<String,Object> vm=new LinkedHashMap<>(); vm.put("quality","source_memcg_inventory"); vm.put("metrics",metrics);
        Map<String,Object> report=report("2026-10-06T22:51:06Z"); report.put("vms",Collections.singletonMap("vm",vm));
        MemoryVmAccountingMetricsInventory typed=MemoryObservationRules.vm("host","vm",report,1791327067000L,90000L).getMetrics();
        assertEquals(Long.valueOf(0),typed.getZero_pages()); assertNull(typed.getCompressed_pages());
        assertEquals(Long.valueOf(4096),typed.getRam_original_bytes()); assertEquals(Double.valueOf(2.5),typed.getRam_logical_to_payload_ratio());
        metrics.put("zero_pages",0.5); MemoryVmAccountingInventory bad=MemoryObservationRules.vm("host","vm",report,1791327067000L,90000L);
        assertEquals("unavailable",bad.getQuality()); assertEquals("VM_ACCOUNTING_METRICS_INVALID",bad.getReason()); assertNull(bad.getMetrics());
    }

    @Test public void nonFiniteSchemaAndPidAreRejectedInsteadOfBecomingNullMetadata() {
        Map<String,Object> badSchema=report("2026-10-06T22:51:06Z");
        badSchema.put("schema_version", Double.NaN);
        MemoryVmAccountingInventory schemaResult=MemoryObservationRules.vm("host","vm",badSchema,1791327067000L,90000L);
        assertEquals("unavailable",schemaResult.getQuality());
        assertEquals("OBSERVATION_METADATA_INVALID",schemaResult.getReason());

        Map<String,Object> badPid=report("2026-10-06T22:51:06Z");
        Map<String,Object> identity=new LinkedHashMap<>(); identity.put("pid",Double.POSITIVE_INFINITY);
        Map<String,Object> entry=new LinkedHashMap<>(); entry.put("quality","source_memcg_inventory");
        entry.put("identity",identity); entry.put("metrics",Collections.emptyMap());
        badPid.put("vms",Collections.singletonMap("vm",entry));
        MemoryVmAccountingInventory pidResult=MemoryObservationRules.vm("host","vm",badPid,1791327067000L,90000L);
        assertEquals("unavailable",pidResult.getQuality()); assertEquals("VM_IDENTITY_INVALID",pidResult.getReason());
    }

    @Test public void nonvalidAgentRowKeepsItsReasonWhenMetricsAreIntentionallyAbsent() {
        Map<String,Object> report=report("2026-10-06T22:51:06Z");
        Map<String,Object> row=new LinkedHashMap<>(); row.put("quality","unsupported"); row.put("reason","KERNEL_CAPABILITY_UNAVAILABLE");
        report.put("vms",Collections.singletonMap("vm",row));
        MemoryVmAccountingInventory result=MemoryObservationRules.vm("host","vm",report,1791327067000L,90000L);
        assertEquals("unsupported",result.getQuality());
        assertEquals("KERNEL_CAPABILITY_UNAVAILABLE",result.getReason());
        assertNull(result.getMetrics());
    }

    private static Map<String, Object> report(String observedAt) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("observed_at", observedAt);
        Map<String,Object> vm=new LinkedHashMap<>(); vm.put("quality","Fresh"); vm.put("metrics",Collections.emptyMap());
        report.put("vms", Collections.singletonMap("vm", vm));
        return report;
    }
}
