package org.zstack.header.network.l3;

import org.zstack.header.message.NeedReplyMessage;

public class AllocateIpMsg extends NeedReplyMessage implements L3NetworkMessage, IpAllocateMessage {
    private String allocateStrategy;
    private String l3NetworkUuid;
    private String requiredIp;
    private String excludedIp;
    private boolean duplicatedIpAllowed = false;

    public String getRequiredIp() {
        return requiredIp;
    }

    public void setRequiredIp(String requiredIp) {
        this.requiredIp = requiredIp;
    }

    @Override
    public String getAllocatorStrategy() {
        return allocateStrategy;
    }

    public void setAllocateStrategy(String allocateStrategy) {
        this.allocateStrategy = allocateStrategy;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public void setDuplicatedIpAllowed(boolean duplicatedIpAllowed) {
        this.duplicatedIpAllowed = duplicatedIpAllowed;
    }

    @Override
    public boolean isDuplicatedIpAllowed() {
        return duplicatedIpAllowed;
    }

    @Override
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    @Override
    public String getExcludedIp() {
        return excludedIp;
    }

    public void setExcludedIp(String excludedIp) {
        this.excludedIp = excludedIp;
    }
}
