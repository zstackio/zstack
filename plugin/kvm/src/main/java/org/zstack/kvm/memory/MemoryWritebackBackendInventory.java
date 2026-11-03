package org.zstack.kvm.memory;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.*;

/** The disks of a Host that can back ZRAM writeback, and whether a preparation can start now. */
public class MemoryWritebackBackendInventory {
    /** The Agent's inventory statuses as the API names them. */
    private static final Map<String, String> STATUSES = new HashMap<>();
    static {
        STATUSES.put("AVAILABLE", "Available");
        // The disk list is complete; the service state that could not be read is one of the reasons.
        STATUSES.put("PARTIAL", "Available");
        STATUSES.put("DISCONNECTED", "Disconnected");
        STATUSES.put("UNSUPPORTED", "Unsupported");
    }

    private String hostUuid;
    private String status;
    private List<String> reasons;
    private Long observedAt;
    private List<String> preparationReasons;
    private List<MemoryWritebackBackendCandidateInventory> candidates;

    public static MemoryWritebackBackendInventory __example__() {
        String hostUuid = "1106a40096e946839d9aaed1b97d4672";
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("hostUuid", hostUuid);
        state.put("status", "AVAILABLE");
        state.put("reasons", Collections.emptyList());
        state.put("observedAt", "2026-11-03T05:39:21.880000Z");
        state.put("preparationReasons", Collections.emptyList());
        Map<String, Object> candidate = new LinkedHashMap<>();
        candidate.put("identity", "candidate:48e5a935e1c0e0eb31aea15a379969ffe824d0a6dd0d453eb77ad81d913a5fec");
        candidate.put("resourceUuid", null);
        candidate.put("type", "block");
        candidate.put("path", "/dev/disk/by-id/virtio-e4e9ca6be5425a879187");
        candidate.put("device", "/dev/vdb");
        candidate.put("capacityBytes", 10737418240L);
        candidate.put("stableIdentity", Collections.singletonMap("serial", "e4e9ca6be5425a879187"));
        candidate.put("transport", "virtio");
        candidate.put("state", "NEEDS_OFFLINE_PREPARATION");
        candidate.put("reasons", Collections.emptyList());
        state.put("candidates", Collections.singletonList(candidate));
        return fromAgentState(hostUuid, hostUuid, state);
    }

    public static MemoryWritebackBackendInventory fromAgentState(String expectedHostUuid,
                                                                  String responseHostUuid,
                                                                  Map<String, Object> state) {
        if (expectedHostUuid == null || !expectedHostUuid.equals(responseHostUuid)
                || state == null || !expectedHostUuid.equals(state.get("hostUuid"))) {
            throw new IllegalArgumentException("WRITEBACK_INVENTORY_HOST_IDENTITY_MISMATCH");
        }
        MemoryWritebackBackendInventory result = new MemoryWritebackBackendInventory();
        result.hostUuid = expectedHostUuid;
        result.status = named(STATUSES, state.get("status"), "WRITEBACK_INVENTORY_STATUS_UNKNOWN");
        result.reasons = strings(state.get("reasons"));
        result.observedAt = millis(state.get("observedAt"));
        // An Agent that did not decide whether a preparation can start must not read as "it can".
        List<String> preparation = strings(state.get("preparationReasons"));
        result.preparationReasons = preparation != null ? preparation
                : Collections.singletonList("PREPARATION_STATE_UNKNOWN");
        result.candidates = candidates(state.get("candidates"));
        return result;
    }

    private static List<MemoryWritebackBackendCandidateInventory> candidates(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (!(value instanceof List)) {
            throw new IllegalArgumentException("WRITEBACK_CANDIDATES_MALFORMED");
        }
        List<MemoryWritebackBackendCandidateInventory> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            Map<?, ?> candidate = map(item);
            if (candidate == null) {
                throw new IllegalArgumentException("WRITEBACK_CANDIDATE_MALFORMED");
            }
            result.add(MemoryWritebackBackendCandidateInventory.fromMap(candidate));
        }
        return result;
    }

    static Map<?, ?> map(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Map)) {
            throw new IllegalArgumentException("WRITEBACK_OBJECT_MALFORMED");
        }
        return (Map<?, ?>) value;
    }

    static String string(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("WRITEBACK_STRING_FIELD_MALFORMED");
        }
        return (String) value;
    }

    /** A string the Agent reports blank when it does not know it. */
    static String optional(Object value) {
        String text = string(value);
        return text == null || text.trim().isEmpty() ? null : text;
    }

    static String named(Map<String, String> names, Object value, String unknown) {
        String name = names.get(string(value));
        if (name == null) {
            throw new IllegalArgumentException(unknown);
        }
        return name;
    }

    static Long millis(Object value) {
        String text = string(value);
        if (text == null) {
            return null;
        }
        try {
            return Instant.parse(text).toEpochMilli();
        } catch (DateTimeException malformed) {
            throw new IllegalArgumentException("WRITEBACK_OBSERVED_AT_MALFORMED", malformed);
        }
    }
    static Long exactLong(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof Number)) {
            throw new IllegalArgumentException("WRITEBACK_INTEGER_FIELD_MALFORMED");
        }
        if (value instanceof Double || value instanceof Float) {
            double number = ((Number) value).doubleValue();
            double maxExactlyRepresentable = value instanceof Float ? 16777215d : 9007199254740991d;
            if (!Double.isFinite(number) || number != Math.rint(number)
                    || number > maxExactlyRepresentable) {
                throw new IllegalArgumentException("WRITEBACK_INTEGER_PRECISION_UNSAFE");
            }
        }
        try {
            long exact = new BigDecimal(value.toString()).longValueExact();
            if (exact < 0) {
                throw new IllegalArgumentException("WRITEBACK_INTEGER_FIELD_MALFORMED");
            }
            return exact;
        } catch (ArithmeticException | NumberFormatException invalid) {
            throw new IllegalArgumentException("WRITEBACK_INTEGER_FIELD_MALFORMED", invalid);
        }
    }

    static List<String> strings(Object value) {
        if (value == null) {
            return null;
        }
        if (!(value instanceof List)) {
            throw new IllegalArgumentException("WRITEBACK_STRING_LIST_MALFORMED");
        }
        List<String> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            String string = string(item);
            if (string == null) {
                throw new IllegalArgumentException("WRITEBACK_STRING_LIST_MALFORMED");
            }
            result.add(string);
        }
        return result;
    }

    public String getHostUuid() { return hostUuid; }
    public void setHostUuid(String value) { hostUuid = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public List<String> getReasons() { return reasons; }
    public void setReasons(List<String> value) { reasons = value; }
    public Long getObservedAt() { return observedAt; }
    public void setObservedAt(Long value) { observedAt = value; }
    public List<String> getPreparationReasons() { return preparationReasons; }
    public void setPreparationReasons(List<String> value) { preparationReasons = value; }
    public List<MemoryWritebackBackendCandidateInventory> getCandidates() { return candidates; }
    public void setCandidates(List<MemoryWritebackBackendCandidateInventory> value) { candidates = value; }
}
