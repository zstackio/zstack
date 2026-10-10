package org.zstack.kvm.memory;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

/** Operational budgets, configurable through the existing GlobalConfig APIs. */
@GlobalConfigDefinition
public class MemoryOptimizationGlobalConfig {
    public static final String CATEGORY = "memoryOptimization";

    @GlobalConfigValidation(numberGreaterThan = 0, numberLessThan = 2147483648L)
    @GlobalConfigDef(defaultValue = "100", type = Long.class, description = "Host observation batch size")
    public static GlobalConfig HOST_BATCH_SIZE = new GlobalConfig(CATEGORY, "host.batchSize");

    @GlobalConfigValidation(numberGreaterThan = 0, numberLessThan = 2147483648L)
    @GlobalConfigDef(defaultValue = "10", type = Long.class, description = "Concurrent Host configuration tasks")
    public static GlobalConfig DISPATCH_CONCURRENCY = new GlobalConfig(CATEGORY, "dispatch.concurrency");

    @GlobalConfigValidation(numberGreaterThan = 0, numberLessThan = 2147483648L)
    @GlobalConfigDef(defaultValue = "32", type = Long.class, description = "VM binding batch size per Host")
    public static GlobalConfig VM_BIND_BATCH_SIZE = new GlobalConfig(CATEGORY, "vm.bindBatchSize");

    @GlobalConfigValidation(numberGreaterThan = 0, numberLessThan = 2147483648L)
    @GlobalConfigDef(defaultValue = "1048576", type = Long.class, description = "Maximum serialized Agent request bytes")
    public static GlobalConfig REQUEST_BYTES = new GlobalConfig(CATEGORY, "agent.requestBytes");

    @GlobalConfigValidation(numberGreaterThan = 0, numberLessThan = 2147483648L)
    @GlobalConfigDef(defaultValue = "16777216", type = Long.class, description = "Maximum serialized Agent response bytes")
    public static GlobalConfig RESPONSE_BYTES = new GlobalConfig(CATEGORY, "agent.responseBytes");

    @GlobalConfigValidation(numberGreaterThan = 0, numberLessThan = 2147483648L)
    @GlobalConfigDef(defaultValue = "100", type = Long.class, description = "Default query page size")
    public static GlobalConfig QUERY_DEFAULT_PAGE_SIZE = new GlobalConfig(CATEGORY, "query.defaultPageSize");

    @GlobalConfigValidation(numberGreaterThan = 0, numberLessThan = 2147483648L)
    @GlobalConfigDef(defaultValue = "500", type = Long.class, description = "Maximum query page size, not resource count")
    public static GlobalConfig QUERY_MAX_PAGE_SIZE = new GlobalConfig(CATEGORY, "query.maxPageSize");

    @GlobalConfigValidation(numberGreaterThan = 0, numberLessThan = 2147483648L)
    @GlobalConfigDef(defaultValue = "30", type = Long.class, description = "Agent communication timeout seconds")
    public static GlobalConfig COMMUNICATION_SECONDS = new GlobalConfig(CATEGORY, "agent.communicationSeconds");

    @GlobalConfigValidation(numberGreaterThan = 0, numberLessThan = 2147483648L)
    @GlobalConfigDef(defaultValue = "90", type = Long.class, description = "Display sample lifetime seconds; unrelated to control protection")
    public static GlobalConfig DISPLAY_TTL_SECONDS = new GlobalConfig(CATEGORY, "display.ttlSeconds");

    @GlobalConfigValidation(numberGreaterThan = 0, numberLessThan = 2147483648L)
    @GlobalConfigDef(defaultValue = "30", type = Long.class, description = "Same-window aggregation seconds")
    public static GlobalConfig SUMMARY_WINDOW_SECONDS = new GlobalConfig(CATEGORY, "display.windowSeconds");

    private MemoryOptimizationGlobalConfig() { }

    static int positive(GlobalConfig config, int defaultValue) {
        String value = config.value();
        if (value == null) { return defaultValue; } // Before platform GlobalConfig initialization (isolated tests).
        long parsed = Long.parseLong(value);
        if (parsed <= 0 || parsed > Integer.MAX_VALUE) {
            throw new MemoryOperationException("MEMORY_INVALID_RUNTIME_SETTING", "Invalid value for " + config.getName());
        }
        return (int) parsed;
    }

    static int pageSize(Integer requested) {
        int max = positive(QUERY_MAX_PAGE_SIZE, 500);
        int result = requested == null ? Math.min(positive(QUERY_DEFAULT_PAGE_SIZE, 100), max) : requested;
        if (result <= 0 || result > max) {
            throw new MemoryOperationException("MEMORY_INVALID_PAGE_SIZE", "Page size must be between 1 and " + max);
        }
        return result;
    }

    static long displayTtlMillis() { return positive(DISPLAY_TTL_SECONDS, 90) * 1000L; }
    static long summaryWindowMillis() { return positive(SUMMARY_WINDOW_SECONDS, 30) * 1000L; }
}
