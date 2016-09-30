package org.zstack.compute.allocator;

import org.zstack.header.allocator.HostCapacityOverProvisioningManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by frank on 10/19/2015.
 */
public class HostCapacityOverProvisioningManagerImpl implements HostCapacityOverProvisioningManager {
    private double globalMemoryRatio = 1;
    private ConcurrentHashMap<String, Double> hostMemoryRatio = new ConcurrentHashMap<String, Double>();

    @Override
    public void setMemoryGlobalRatio(double ratio) {
        globalMemoryRatio = ratio;
    }

    @Override
    public double getMemoryGlobalRatio() {
        return globalMemoryRatio;
    }

    @Override
    public void setMemoryRatio(String hostUuid, double ratio) {
        hostMemoryRatio.put(hostUuid, ratio);
    }

    @Override
    public void deleteMemoryRatio(String hostUuid) {
        hostMemoryRatio.remove(hostUuid);
    }

    @Override
    public double getMemoryRatio(String hostUuid) {
        Double ratio =  hostMemoryRatio.get(hostUuid);
        ratio = ratio == null ? globalMemoryRatio : ratio;
        return ratio;
    }

    @Override
    public Map<String, Double> getAllMemoryRatio() {
        return hostMemoryRatio;
    }

    @Override
    public long calculateMemoryByRatio(String hostUuid, long capacity) {
        double ratio = getMemoryRatio(hostUuid);
        return Math.round(capacity / ratio);
    }

    @Override
    public long calculateHostAvailableMemoryByRatio(String hostUuid, long capacity) {
        double ratio = getMemoryRatio(hostUuid);
        return Math.round(capacity * ratio);
    }
}
