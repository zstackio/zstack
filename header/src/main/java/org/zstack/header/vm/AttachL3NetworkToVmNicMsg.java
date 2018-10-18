package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by shixin on 10/18/2018.
 */
public class AttachL3NetworkToVmNicMsg extends NeedReplyMessage {
    private String vmNicUuid;
    private String l3NetworkUuid;
    private String staticIp;

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getStaticIp() {
        return staticIp;
    }

    public void setStaticIp(String staticIp) {
        this.staticIp = staticIp;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }
}
