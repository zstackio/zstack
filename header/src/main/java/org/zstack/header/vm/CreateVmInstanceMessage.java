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

    long getReservedMemorySize();

    List<VmNicSpec> getL3NetworkSpecs();

    String getType();

    String getRootDiskOfferingUuid();

    long getRootDiskSize();

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
