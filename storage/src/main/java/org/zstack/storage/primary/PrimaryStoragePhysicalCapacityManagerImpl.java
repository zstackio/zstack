package org.zstack.storage.primary;

import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by frank on 11/9/2015.
 */
public class PrimaryStoragePhysicalCapacityManagerImpl implements PrimaryStoragePhysicalCapacityManager {
    private double globalRatio = 1;
    private ConcurrentHashMap<String, Double> primaryStorageRatio = new ConcurrentHashMap<String, Double>();

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
        return  ratio == null ? globalRatio : ratio;
    }

    @Override
    public Map<String, Double> getAllRatio() {
        return primaryStorageRatio;
    }

    @Override
    public boolean checkCapacityByRatio(String psUuid, long totalPhysicalCapacity, long totalAvailableCapacity) {
        if (totalAvailableCapacity == totalPhysicalCapacity) {
            return true;
        }

        double ratio = getRatio(psUuid);
        long used = totalPhysicalCapacity - totalAvailableCapacity;
        return used < Math.round(totalPhysicalCapacity * ratio);
    }

    @Override
    public boolean checkCapacityByRatio(String psUuid, PrimaryStorageCapacityVO cap) {
        return checkCapacityByRatio(psUuid, cap.getTotalPhysicalCapacity(), cap.getAvailablePhysicalCapacity());
    }
}
