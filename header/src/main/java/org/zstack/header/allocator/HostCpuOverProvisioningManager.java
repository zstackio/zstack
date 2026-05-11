package org.zstack.header.allocator;

import java.util.Map;

/**
 * Created by xing5 on 2016/5/12.
 */
public interface HostCpuOverProvisioningManager {
    void setGlobalRatio(int ratio);

    int getGlobalRatio();

    void setRatio(String hostUuid, int ratio);

    void deleteRatio(String hostUuid);

    int getRatio(String hostUuid);

    Map<String, Integer> getAllRatio();

    int calculateByRatio(String hostUuid, int cpuNum);

    int calculateHostCpuByRatio(String hostUuid, int cpuNum);

    /**
     * Refresh {@code PhysicalServerCapacityVO.totalCpu} for the given host using the supplied
     * ratio, then trigger a recalculate. Distinct from {@link #setRatio} in that it does
     * <b>not</b> touch the in-memory per-host ratios cache — for callers that want the JPQL-side
     * effect (e.g. ResourceConfig hierarchy listeners) but still expect {@link #getRatio} to
     * walk the ResourceConfig stack rather than read the cache.
     */
    void refreshHostCpuCapacity(String hostUuid, int ratio);
}
