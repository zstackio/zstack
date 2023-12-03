package org.zstack.network.service.flat;

public interface L3NetworkGetIpStatisticExtensionPoint {
    String getApplianceVmInstanceType();
    String getParentUuid(String instanceUuid);
    String getParentUuid(String instanceUuid, String vipUuid);
}
