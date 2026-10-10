package org.zstack.kvm.memory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Explicit MN projection of the Agent operation journal. Unknown service keys never pass through. */
public class MemoryHostOperationsInventory {
    private String hostUuid;
    private List<Entry> entries;
    private Long total;
    private Integer nextPage;
    private Concurrency concurrency;
    private RecordBudget recordBudget;

    public String getHostUuid() { return hostUuid; }
    public void setHostUuid(String value) { hostUuid = value; }
    public List<Entry> getEntries() { return entries; }
    public void setEntries(List<Entry> value) { entries = value; }
    public Long getTotal() { return total; }
    public void setTotal(Long value) { total = value; }
    public Integer getNextPage() { return nextPage; }
    public void setNextPage(Integer value) { nextPage = value; }
    public Concurrency getConcurrency() { return concurrency; }
    public void setConcurrency(Concurrency value) { concurrency = value; }
    public RecordBudget getRecordBudget() { return recordBudget; }
    public void setRecordBudget(RecordBudget value) { recordBudget = value; }
    /** @deprecated internal Java source compatibility only; not serialized as a public field. */
    @Deprecated public Concurrency getCq() { return concurrency; }
    /** @deprecated internal Java source compatibility only; not serialized as a public field. */
    @Deprecated public RecordBudget getRecord_budget() { return recordBudget; }

    public static MemoryHostOperationsInventory __example__() {
        MemoryHostOperationsInventory inventory = new MemoryHostOperationsInventory();
        inventory.hostUuid = "1234567890abcdef1234567890abcdef";
        Entry writeback = Entry.__example__();
        inventory.entries = new ArrayList<>();
        inventory.entries.add(writeback);
        inventory.total = 1L;
        inventory.nextPage = null;
        inventory.concurrency = Concurrency.__example__();
        inventory.recordBudget = RecordBudget.__example__();
        return inventory;
    }

    @SuppressWarnings("unchecked")
    public static MemoryHostOperationsInventory fromAgentState(String expectedHost, String responseHostUuid,
                                                                Map<String, Object> raw) {
        if (expectedHost == null || raw == null || !expectedHost.equals(responseHostUuid)) {
            throw unavailable();
        }

        MemoryHostOperationsInventory result = new MemoryHostOperationsInventory();
        result.hostUuid = expectedHost;
        Object rawEntries = raw.get("entries");
        if (!(rawEntries instanceof List)) {
            throw unavailable();
        }

        result.entries = new ArrayList<>();
        for (Object rawEntry : (List<?>) rawEntries) {
            if (!(rawEntry instanceof Map)) {
                throw unavailable();
            }
            Map<String, Object> entry = (Map<String, Object>) rawEntry;
            Entry projected = new Entry();
            projected.operationId = requiredString(entry, "operation_id");
            // Writeback entries are pool-wide and intentionally have an empty vm_uuid.
            Object vmUuid = entry.get("vm_uuid");
            if (!(vmUuid instanceof String)) {
                throw unavailable();
            }
            projected.vmUuid = (String) vmUuid;
            projected.kind = requiredString(entry, "kind");
            projected.status = requiredString(entry, "status");
            projected.startedAt = requiredTimestamp(entry, "started_at");
            projected.timeoutAt = optionalTimestamp(entry, "timeout_at");
            projected.completedAt = optionalTimestamp(entry, "completed_at");
            projected.reason = optionalString(entry, "reason");
            result.entries.add(projected);
        }

        result.total = integer(raw.get("total"));
        result.nextPage = nullableInt(raw.get("nextPage"));
        Map<String, Object> concurrency = requiredMap(raw, "cq");
        result.concurrency = new Concurrency();
        result.concurrency.activeOperations = requiredInt(concurrency.get("C"));
        result.concurrency.isolatedTimeoutOperations = requiredInt(concurrency.get("Q"));

        // The service omits record_budget when no budget tracker is configured.
        Object rawBudget = raw.get("record_budget");
        if (rawBudget != null) {
            Map<String, Object> budget = requiredMap(raw, "record_budget");
            result.recordBudget = new RecordBudget();
            result.recordBudget.actualAllocatedBytes = integer(budget.get("actual_allocated_bytes"));
            result.recordBudget.reservedBytes = integer(budget.get("reserved_bytes"));
            result.recordBudget.budgetBytes = integer(budget.get("budget_bytes"));
            result.recordBudget.records = requiredInt(budget.get("records"));
            result.recordBudget.protectedRecords = requiredInt(budget.get("protected_records"));
        }
        return result;
    }

    private static MemoryOperationException unavailable() {
        return new MemoryOperationException("MEMORY_OPERATION_QUERY_UNAVAILABLE",
                "Malformed Host operation observation");
    }

    private static String requiredString(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (!(value instanceof String) || ((String) value).isEmpty()) {
            throw unavailable();
        }
        return (String) value;
    }

    private static String requiredTimestamp(Map<String, Object> values, String key) {
        String value = requiredString(values, key);
        validateTimestamp(value);
        return value;
    }

    private static String optionalTimestamp(Map<String, Object> values, String key) {
        String value = optionalString(values, key);
        if (value != null) {
            validateTimestamp(value);
        }
        return value;
    }

    private static void validateTimestamp(String value) {
        try {
            java.time.Instant.parse(value);
        } catch (java.time.DateTimeException utcParseFailure) {
            try {
                java.time.OffsetDateTime.parse(value);
            } catch (java.time.DateTimeException offsetParseFailure) {
                throw unavailable();
            }
        }
    }

    private static String optionalString(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value == null) {
            return null;
        }
        if (!(value instanceof String)) {
            throw unavailable();
        }
        return (String) value;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> requiredMap(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (!(value instanceof Map)) {
            throw unavailable();
        }
        return (Map<String, Object>) value;
    }

    private static Long integer(Object value) {
        if (!(value instanceof Number)) {
            throw unavailable();
        }
        try {
            if (value instanceof Double) {
                double number = (Double) value;
                if (!Double.isFinite(number) || number != Math.rint(number)
                        || Math.abs(number) > 9007199254740991d) {
                    throw unavailable();
                }
            }
            if (value instanceof Float) {
                float number = (Float) value;
                if (!Float.isFinite(number) || number != Math.rint(number) || Math.abs(number) > 16777215f) {
                    throw unavailable();
                }
            }
            long number = new BigDecimal(value.toString()).longValueExact();
            if (number < 0) {
                throw unavailable();
            }
            return number;
        } catch (ArithmeticException | NumberFormatException invalidNumber) {
            throw unavailable();
        }
    }

    private static Integer nullableInt(Object value) {
        if (value == null) {
            return null;
        }
        Long number = integer(value);
        if (number > Integer.MAX_VALUE) {
            throw unavailable();
        }
        return number.intValue();
    }

    private static Integer requiredInt(Object value) {
        Integer number = nullableInt(value);
        if (number == null) {
            throw unavailable();
        }
        return number;
    }

    public static class Entry {
        private String operationId;
        private String vmUuid;
        private String kind;
        private String status;
        private String startedAt;
        private String timeoutAt;
        private String completedAt;
        private String reason;

        public static Entry __example__() {
            Entry entry = new Entry();
            entry.operationId = "op-20261010-0001";
            entry.vmUuid = "";
            entry.kind = "writeback";
            entry.status = "succeeded";
            entry.startedAt = "2026-10-10T01:59:00Z";
            entry.timeoutAt = "2026-10-10T02:00:00Z";
            entry.completedAt = "2026-10-10T01:59:08Z";
            entry.reason = null;
            return entry;
        }

        public String getOperationId() { return operationId; }
        public String getVmUuid() { return vmUuid; }
        public String getKind() { return kind; }
        public String getStatus() { return status; }
        public String getStartedAt() { return startedAt; }
        public String getTimeoutAt() { return timeoutAt; }
        public String getCompletedAt() { return completedAt; }
        public String getReason() { return reason; }
        /** @deprecated internal Java source compatibility only. */
        @Deprecated public String getOperation_id() { return operationId; }
        /** @deprecated internal Java source compatibility only. */
        @Deprecated public String getVm_uuid() { return vmUuid; }
        /** @deprecated internal Java source compatibility only. */
        @Deprecated public String getStarted_at() { return startedAt; }
        /** @deprecated internal Java source compatibility only. */
        @Deprecated public String getTimeout_at() { return timeoutAt; }
        /** @deprecated internal Java source compatibility only. */
        @Deprecated public String getCompleted_at() { return completedAt; }
    }

    public static class Concurrency {
        private Integer activeOperations;
        private Integer isolatedTimeoutOperations;

        public static Concurrency __example__() {
            Concurrency concurrency = new Concurrency();
            concurrency.activeOperations = 1;
            concurrency.isolatedTimeoutOperations = 0;
            return concurrency;
        }

        public Integer getActiveOperations() { return activeOperations; }
        public Integer getIsolatedTimeoutOperations() { return isolatedTimeoutOperations; }
        /** @deprecated internal Java source compatibility only. */
        @Deprecated public Integer getC() { return activeOperations; }
        /** @deprecated internal Java source compatibility only. */
        @Deprecated public Integer getQ() { return isolatedTimeoutOperations; }
    }

    public static class RecordBudget {
        private Long actualAllocatedBytes;
        private Long reservedBytes;
        private Long budgetBytes;
        private Integer records;
        private Integer protectedRecords;

        public static RecordBudget __example__() {
            RecordBudget budget = new RecordBudget();
            budget.actualAllocatedBytes = 32768L;
            budget.reservedBytes = 16384L;
            budget.budgetBytes = 1073741824L;
            budget.records = 12;
            budget.protectedRecords = 2;
            return budget;
        }

        public Long getActualAllocatedBytes() { return actualAllocatedBytes; }
        public Long getReservedBytes() { return reservedBytes; }
        public Long getBudgetBytes() { return budgetBytes; }
        public Integer getRecords() { return records; }
        public Integer getProtectedRecords() { return protectedRecords; }
        /** @deprecated internal Java source compatibility only. */
        @Deprecated public Long getActual_allocated_bytes() { return actualAllocatedBytes; }
        /** @deprecated internal Java source compatibility only. */
        @Deprecated public Long getReserved_bytes() { return reservedBytes; }
        /** @deprecated internal Java source compatibility only. */
        @Deprecated public Long getBudget_bytes() { return budgetBytes; }
        /** @deprecated internal Java source compatibility only. */
        @Deprecated public Integer getProtected_records() { return protectedRecords; }
    }
}
