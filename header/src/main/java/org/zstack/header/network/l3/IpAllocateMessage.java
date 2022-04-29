package org.zstack.header.network.l3;

public interface IpAllocateMessage {
    String getAllocatorStrategy();

    String getL3NetworkUuid();

    String getRequiredIp();

    String getIpRangeUuid();

    default String getExcludedIp() {
        return null;
    }
    default boolean isDuplicatedIpAllowed() {return false;}
    default boolean isUseAddressPoolIfNotRequiredIpRange() { return false; }
}
