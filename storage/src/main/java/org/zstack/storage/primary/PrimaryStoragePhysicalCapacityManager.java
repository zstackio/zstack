package org.zstack.storage.primary;

import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;

import java.util.Map;

/**
 * Created by frank on 11/9/2015.
 */
public interface PrimaryStoragePhysicalCapacityManager {
    void setGlobalRatio(double ratio);

    double getGlobalRatio();

    void setRatio(String psUuid, double ratio);

    void deleteRatio(String psUuid);

    double getRatio(String psUuid);

    Map<String, Double> getAllRatio();

    boolean checkCapacityByRatio(String psUuid, long totalPhysicalCapacity, long totalAvailableCapacity);

    boolean checkCapacityByRatio(String psUuid, PrimaryStorageCapacityVO cap);
}
