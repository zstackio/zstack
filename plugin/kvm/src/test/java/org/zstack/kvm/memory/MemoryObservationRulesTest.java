package org.zstack.kvm.memory;

import org.junit.Test;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.time.OffsetDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MemoryObservationRulesTest {
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
