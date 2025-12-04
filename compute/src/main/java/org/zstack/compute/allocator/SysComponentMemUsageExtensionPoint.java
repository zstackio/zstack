package org.zstack.compute.allocator;

public interface SysComponentMemUsageExtensionPoint {
    /**
     * Retrieves the amount of huge page memory used by the system component (in byte).
     *
     * @return the number of byte of huge page memory in use
     */
    long getHugePageMemoryUsage(String hostUuid);

    /**
     * Retrieves the amount of normal memory used by the system component (in byte).
     *
     * @return the number of byte of normal memory in use
     */
    long getNormalMemoryUsage(String hostUuid);
}
