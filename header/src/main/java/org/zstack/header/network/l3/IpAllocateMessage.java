package org.zstack.header.network.l3;

public interface IpAllocateMessage {
    String getAllocatorStrategy();

    String getL3NetworkUuid();

    String getRequiredIp();

    default String getExcludedIp() {
        return null;
    }
}
