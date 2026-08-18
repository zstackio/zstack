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

    default String getNetmask() {
        return null;
    }

    default String getGateway() {
        return null;
    }

    default String getIpv6Gateway() {
        return null;
    }

    default String getIpv6Prefix() {
        return null;
    }

    default String getOperationUuid() {
        return null;
    }

    default String getOperationStep() {
        return null;
    }

    void setIpRangeUuid(String ipRangeUuid);

    void setRequiredIp(String requiredIp);
}
