package org.zstack.header.network.l3;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by frank on 1/21/2016.
 */
public class CheckIpAvailabilityMsg extends NeedReplyMessage implements L3NetworkMessage {
    private String l3NetworkUuid;
    private String ip;
    private Boolean arpingDetection= false;

    @Override
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Boolean getArpingDetection() {
        return arpingDetection;
    }

    public void setArpingDetection(Boolean arpingDetection) {
        this.arpingDetection = arpingDetection;
    }
}
