package org.zstack.storage.primary;

import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by frank on 10/19/2015.
 */
public class PrimaryStorageOverProvisioningManagerImpl implements PrimaryStorageOverProvisioningManager {
    private double globalRatio = 1;
    private ConcurrentHashMap<String, Double> primaryStorageRatio = new ConcurrentHashMap<>();

    @Override
    public void setGlobalRatio(double ratio) {
        globalRatio = ratio;
    }

    @Override
    public double getGlobalRatio() {
        return globalRatio;
    }

    @Override
    public void setRatio(String psUuid, double ratio) {
        primaryStorageRatio.put(psUuid, ratio);
    }

    @Override
    public void deleteRatio(String psUuid) {
        primaryStorageRatio.remove(psUuid);
    }

    @Override
    public double getRatio(String psUuid) {
        Double ratio = primaryStorageRatio.get(psUuid);
        return ratio == null ? globalRatio : ratio;
    }

    @Override
    public Map<String, Double> getAllRatio() {
        return primaryStorageRatio;
    }

    @Override
    public long calculateByRatio(String psUuid, long capacity) {
        double ratio = getRatio(psUuid);
        return Math.round(capacity / ratio);
    }

    @Override
    public long calculatePrimaryStorageAvailableCapacityByRatio(String psUuid, long capacity) {
        double ratio = getRatio(psUuid);
        return Math.round(capacity * ratio);
    }
}
