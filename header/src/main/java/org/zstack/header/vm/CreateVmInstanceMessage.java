package org.zstack.header.vm;

import java.util.List;

/**
 * Created by david on 8/22/16.
 */
public interface CreateVmInstanceMessage {
    String getInstanceOfferingUuid();

    String getAccountUuid();

    String getName();

    String getImageUuid();

    int getCpuNum();

    long getCpuSpeed();

    long getMemorySize();

    List<String> getL3NetworkUuids();

    String getType();

    String getRootDiskOfferingUuid();

    List<String> getDataDiskOfferingUuids();

    String getZoneUuid();

    String getClusterUuid();

    String getHostUuid();

    String getDescription();

    String getResourceUuid();

    String getDefaultL3NetworkUuid();

    String getAllocatorStrategy();

    String getStrategy(); // VmCreationStrategy
}
