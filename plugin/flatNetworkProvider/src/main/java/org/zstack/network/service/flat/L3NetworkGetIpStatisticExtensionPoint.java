package org.zstack.network.service.flat;

public interface L3NetworkGetIpStatisticExtensionPoint {
    String getType();
    String getParentUuid(String instanceUuid);
    String getParentUuid(String instanceUuid, String vipUuid);
    String getResourceOwnerUuid(String usedIpUuid);
    Long countUsedIp(String l3NetworkUuid, String ip);
}
