package org.zstack.kvm.memory;

import org.junit.Test;
import org.zstack.utils.gson.JSONObjectUtil;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.time.OffsetDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MemoryObservationRulesTest {
    private static MemoryStateVO desired(String json) {
        MemoryStateVO state = new MemoryStateVO(); state.setHostUuid("host"); state.setDesired(json); return state;
    }

    private static final String KSM_ENABLED = "{\"control\":\"active\",\"ksm\":{\"enabled\":true}}";
    private static final String FAILED_KSM = "{\"ksm\":{\"serviceState\":\"failed\",\"tunedServiceState\":\"active\"}}";

    @Test public void serviceEventsReportChangesNotEverySample() {
        long now = System.currentTimeMillis();
        MemoryStateVO policy = desired(KSM_ENABLED);
        MemoryAgentResponse response = serviceObservation(now);
        Map<String, Object> ksm = new LinkedHashMap<>();
        ksm.put("serviceState", "failed"); ksm.put("tunedServiceState", "active");
        response.state.put("ksm", ksm);
        // Every 30 seconds the same sample arrives again; only the change is an event.
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, FAILED_KSM, now).size());
        ksm.put("serviceState", "active");
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy,
                "{\"ksm\":{\"serviceState\":\"active\",\"tunedServiceState\":\"active\"}}", now).size());
    }

    @Test public void nativeServiceEventsRequireExplicitFailedOrActiveNotScanningOrUnknown() {
        long now = System.currentTimeMillis();
        MemoryStateVO policy = desired(KSM_ENABLED);
        MemoryAgentResponse response = serviceObservation(now);
        Map<String, Object> ksm = new LinkedHashMap<>();
        response.state.put("ksm", ksm);
        ksm.put("enabled", null); ksm.put("scanning", false);
        ksm.put("serviceState", "failed"); ksm.put("tunedServiceState", "activating");
        assertEquals(1, MemoryObservationRules.serviceEvents("host", response, policy, null, now).size());
        assertEquals("Failed", MemoryObservationRules.serviceEvents("host", response, policy, null, now).get(0).getStatus());
        ksm.put("serviceState", "active");
        assertEquals("Recovered", MemoryObservationRules.serviceEvents("host", response, policy, FAILED_KSM, now).get(0).getStatus());
        for (Object state : new Object[]{null, "inactive", "activating", "deactivating", "unknown"}) {
            ksm.put("serviceState", state);
            assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, null, now).size());
        }
        ksm.put("tunedServiceState", "failed");
        assertEquals("ksmtuned", MemoryObservationRules.serviceEvents("host", response, policy, null, now).get(0).getService());
        response.setSuccess(false);
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, null, now).size());
        response.setSuccess(true);
        for (String json : new String[]{"{\"control\":\"active\",\"ksm\":{\"enabled\":false}}",
                "{\"control\":\"active\",\"ksm\":{}}", "{\"control\":\"paused\",\"ksm\":{\"enabled\":true}}"}) {
            assertEquals(json, 0, MemoryObservationRules.serviceEvents("host", response, desired(json), null, now).size());
        }
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, null, null, now).size());
    }

    @Test public void serviceEventsRejectStaleForeignAndPausedSamples() {
        long now = System.currentTimeMillis();
        MemoryStateVO policy = desired(KSM_ENABLED);
        MemoryAgentResponse response = serviceObservation(now);
        response.state.put("ksm", Collections.singletonMap("serviceState", "failed"));
        assertEquals(1, MemoryObservationRules.serviceEvents("host", response, policy, null, now).size());
        response.state.put("sampleTime", now - MemoryOptimizationGlobalConfig.displayTtlMillis());
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, null, now).size());
        response.state.put("sampleTime", now + 6000);
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, null, now).size());
        response.state.put("sampleTime", now); response.hostUuid = "other-host";
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, null, now).size());
        response.hostUuid = "host"; response.state.put("control", "paused");
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, null, now).size());
    }

    @Test public void writebackFailureDoesNotTreatUnknownFenceOrHistoricalBaselineAsIoFailure() {
        long now = OffsetDateTime.parse("2026-10-13T00:00:00Z").toInstant().toEpochMilli();
        MemoryAgentResponse response = writebackObservation(now);
        MemoryStateVO policy = desired("{\"control\":\"active\",\"zram\":{\"enabled\":true},\"writeback\":{\"enabled\":true}}");
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("state", "DEGRADED"); health.put("reason", "NATIVE_BACKEND_IO_ERROR_BACKOFF");
        health.put("new_io_permitted_by_backend_health", false); status(response).put("backend_health", health);
        assertEquals("Failed", MemoryObservationRules.serviceEvents("host", response, policy, null, now).get(0).getStatus());
        String failed = JSONObjectUtil.toJsonString(response.state);
        // Each further error moves the backoff; the backend is still the same failure.
        health.put("backoff_until", "2026-10-13T00:01:00Z");
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, failed, now).size());
        health.remove("backoff_until");
        for (String reason : new String[]{"UNKNOWN_OPERATION_NO_REPLAY", "NATIVE_ERROR_BASELINE_RECORDED", "NO_BACKEND"}) {
            health.put("reason", reason);
            assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, null, now).size());
        }
        health.put("reason", "NATIVE_BACKEND_IO_ERROR_BACKOFF");
        status(response).put("observed_at", "2020-01-01T00:00:00Z");
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, null, now).size());
        status(response).put("observed_at", "2026-10-13T00:00:00Z"); status(response).put("resource_authorized", false);
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, null, now).size());
    }

    @Test public void writebackRecoveryRequiresSuccessfulIoAfterBackoffNotIdleOrElapsedTimeAlone() {
        long now = OffsetDateTime.parse("2026-10-13T00:00:00Z").toInstant().toEpochMilli();
        MemoryAgentResponse response = writebackObservation(now);
        MemoryStateVO policy = desired("{\"control\":\"active\",\"zram\":{\"enabled\":true},\"writeback\":{\"enabled\":true}}");
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("state", "READY"); health.put("reason", "BACKOFF_ELAPSED_BOUNDED_IO_PERMITTED");
        health.put("new_io_permitted_by_backend_health", true); health.put("backoff_until", "2026-10-12T23:59:50Z");
        status(response).put("backend_health", health);
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, null, now).size());
        Map<String, Object> schedule = new LinkedHashMap<>(); schedule.put("pool_generation", "pool-1");
        schedule.put("last_completed_id", "operation-1"); status(response).put("writeback_schedule", schedule);
        Map<String, Object> pool = new LinkedHashMap<>(); pool.put("pool_generation", "pool-1");
        pool.put("quality", "native_host_device"); status(response).put("host_zram", pool);
        Map<String, Object> before = new LinkedHashMap<>(); before.put("quality", "native_host_device");
        before.put("observed_at", "2026-10-12T23:59:51Z");
        before.put("committed_writeback_pages_total", 10L); before.put("read_errors", 1L); before.put("write_errors", 2L);
        Map<String, Object> after = new LinkedHashMap<>(before);
        Map<String, Object> returned = new LinkedHashMap<>(); returned.put("operation_id", "operation-1");
        returned.put("returned_at", "2026-10-12T23:59:52Z"); returned.put("kernel_called", true);
        returned.put("before", before); returned.put("after", after); status(response).put("last_writeback_return", returned);
        assertEquals("idle/no progress is not recovery", 0, MemoryObservationRules.serviceEvents("host", response, policy, null, now).size());
        String idle = JSONObjectUtil.toJsonString(response.state);
        after.put("committed_writeback_pages_total", 11L);
        assertEquals("Recovered", MemoryObservationRules.serviceEvents("host", response, policy, null, now).get(0).getStatus());
        // The backend health stays the same when the first writeback after the backoff succeeds.
        assertEquals("Recovered", MemoryObservationRules.serviceEvents("host", response, policy, idle, now).get(0).getStatus());
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy,
                JSONObjectUtil.toJsonString(response.state), now).size());
        after.put("write_errors", 3L);
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, null, now).size());
        after.put("write_errors", 2L); returned.put("error", "failed");
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, null, now).size());
        returned.remove("error"); returned.put("kernel_called", false);
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, null, now).size());
        returned.put("kernel_called", true); health.put("backoff_until", "2026-10-12T23:59:59Z");
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, null, now).size());
        health.put("backoff_until", "2026-10-12T23:59:50Z"); schedule.put("pool_generation", "old-pool");
        assertEquals(0, MemoryObservationRules.serviceEvents("host", response, policy, null, now).size());
    }

    private static MemoryAgentResponse serviceObservation(long now) {
        MemoryAgentResponse response = new MemoryAgentResponse(); response.setSuccess(true); response.status = "Succeeded";
        response.state = new LinkedHashMap<>(); response.state.put("sampleTime", now); response.state.put("control", "active");
        return response;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> status(MemoryAgentResponse response) {
        return (Map<String, Object>) ((Map<String, Object>) response.state.get("zram")).get("status");
    }

    private static MemoryAgentResponse writebackObservation(long now) {
        MemoryAgentResponse response = serviceObservation(now);
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("resource_authorized", true); status.put("observed_at", "2026-10-13T00:00:00Z");
        response.state.put("zram", new LinkedHashMap<>(Collections.singletonMap("status", status)));
        return response;
    }

    @Test public void exactLongAcceptsOnlyExactIntegersWithoutThrowing() {
        assertEquals(Long.valueOf(-4096), MemoryObservationRules.exactLong(-4096d));
        assertEquals(Long.valueOf(7), MemoryObservationRules.exactLong(7));
        assertEquals(Long.valueOf(9007199254740993L), MemoryObservationRules.exactLong(new java.math.BigInteger("9007199254740993")));
        assertEquals(Long.valueOf(1200), MemoryObservationRules.exactLong(new java.math.BigDecimal("1.2E3")));
        for (Object invalid : java.util.Arrays.<Object>asList(null, "7", 1.5d, Double.NaN, 9007199254740992d,
                new java.math.BigDecimal("1E30"), new java.math.BigDecimal("0.5"))) {
            assertNull("not an exact long: " + invalid, MemoryObservationRules.exactLong(invalid));
        }
        assertNull("an exponent beyond any long", MemoryObservationRules.parseExactLong("1e99999999999"));
    }

    @Test public void epochMillisReadsRfc3339AndRejectsImpossibleTimesWithoutThrowing() {
        assertEquals(Long.valueOf(1791468260720L), MemoryObservationRules.epochMillis("2026-10-08T14:04:20.720910Z"));
        assertEquals(Long.valueOf(1791468260000L), MemoryObservationRules.epochMillis("2026-10-08T22:04:20+08:00"));
        assertEquals(Long.valueOf(1709164800000L), MemoryObservationRules.epochMillis("2024-02-29T00:00:00Z"));
        assertEquals("zero seconds may be left out", Long.valueOf(1800000900000L), MemoryObservationRules.epochMillis("2027-01-15T08:15Z"));
        for (String invalid : java.util.Arrays.asList("2026-02-29T00:00:00Z", "2026-13-01T00:00:00Z", "2026-10-08T24:00:00Z",
                "2026-10-08T14:04:20", "2026-10-08T14:04:20+19:00", "yesterday", "")) {
            assertNull("not an RFC 3339 time: " + invalid, MemoryObservationRules.epochMillis(invalid));
        }
        assertNull(MemoryObservationRules.epochMillis(1791468260720L));
    }
}
