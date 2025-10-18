package org.zstack.compute.allocator;

public interface HugePageMemoryUsageExtensionPoint {
    /**
     * Retrieves the amount of huge page memory used by the system component (in KB).
     *
     * @return the number of KB of huge page memory in use
     */
    long getHugePageMemoryUsage(String hostUuid);
}
