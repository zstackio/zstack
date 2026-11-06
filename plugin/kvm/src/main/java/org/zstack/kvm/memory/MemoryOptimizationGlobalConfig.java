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
    @GlobalConfigDef(defaultValue = "32", type = Long.class, description = "VM binding batch size per Host")
    public static GlobalConfig VM_BIND_BATCH_SIZE = new GlobalConfig(CATEGORY, "vm.bindBatchSize");

    @GlobalConfigValidation(numberGreaterThan = 0, numberLessThan = 2147483648L)
    @GlobalConfigDef(defaultValue = "1048576", type = Long.class, description = "Maximum serialized Agent request bytes")
    public static GlobalConfig REQUEST_BYTES = new GlobalConfig(CATEGORY, "agent.requestBytes");

    @GlobalConfigValidation(numberGreaterThan = 0, numberLessThan = 2147483648L)
    @GlobalConfigDef(defaultValue = "16777216", type = Long.class, description = "Maximum serialized Agent response bytes")
    public static GlobalConfig RESPONSE_BYTES = new GlobalConfig(CATEGORY, "agent.responseBytes");

    @GlobalConfigValidation(numberGreaterThan = 0, numberLessThan = 2147483648L)
    @GlobalConfigDef(defaultValue = "30", type = Long.class, description = "Agent communication timeout seconds")
    public static GlobalConfig COMMUNICATION_SECONDS = new GlobalConfig(CATEGORY, "agent.communicationSeconds");

    @GlobalConfigValidation(numberGreaterThan = 0, numberLessThan = 2147483648L)
    @GlobalConfigDef(defaultValue = "90", type = Long.class, description = "Display sample lifetime seconds; unrelated to control protection")
    public static GlobalConfig DISPLAY_TTL_SECONDS = new GlobalConfig(CATEGORY, "display.ttlSeconds");

    @GlobalConfigValidation(numberGreaterThan = 0, numberLessThan = 2147483648L)
    @GlobalConfigDef(defaultValue = "30", type = Long.class, description = "Same-window aggregation seconds")
    public static GlobalConfig SUMMARY_WINDOW_SECONDS = new GlobalConfig(CATEGORY, "display.windowSeconds");

    @GlobalConfigValidation(numberGreaterThan = 0, numberLessThan = 2147483648L)
    @GlobalConfigDef(defaultValue = "30", type = Long.class, description = "Seconds before a failed Host configuration is sent again; each further failure doubles it, up to 10 minutes")
    public static GlobalConfig RETRY_SECONDS = new GlobalConfig(CATEGORY, "retry.firstIntervalSeconds");

    @GlobalConfigValidation(numberGreaterThan = 0, numberLessThan = 2147483648L)
    @GlobalConfigDef(defaultValue = "604800", type = Long.class, description = "Seconds a finished memory optimization operation is kept")
    public static GlobalConfig HISTORY_RETENTION_SECONDS = new GlobalConfig(CATEGORY, "history.retentionSeconds");

    @GlobalConfigValidation(numberGreaterThan = 0, numberLessThan = 2147483648L)
    @GlobalConfigDef(defaultValue = "3600", type = Long.class, description = "Seconds between purges of finished memory optimization operations")
    public static GlobalConfig HISTORY_PURGE_SECONDS = new GlobalConfig(CATEGORY, "history.purgeIntervalSeconds");

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

    static long displayTtlMillis() { return positive(DISPLAY_TTL_SECONDS, 90) * 1000L; }

    /** Hosts observed per 30-second pass: every Host within two passes, so inside the display sample lifetime. */
    static int observationBatch(int hosts) {
        return Math.max(positive(HOST_BATCH_SIZE, 100), (hosts + 1) / 2);
    }
    static long summaryWindowMillis() { return positive(SUMMARY_WINDOW_SECONDS, 30) * 1000L; }
    static long firstRetryMillis() { return positive(RETRY_SECONDS, 30) * 1000L; }
    static long historyRetentionMillis() { return positive(HISTORY_RETENTION_SECONDS, 604800) * 1000L; }
    static long historyPurgeMillis() { return positive(HISTORY_PURGE_SECONDS, 3600) * 1000L; }
}
