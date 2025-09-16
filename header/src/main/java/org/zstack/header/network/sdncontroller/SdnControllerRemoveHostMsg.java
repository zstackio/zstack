package org.zstack.header.network.sdncontroller;

import org.zstack.header.message.NeedReplyMessage;

public class SdnControllerRemoveHostMsg extends NeedReplyMessage implements SdnControllerMessage {
    private String sdnControllerUuid;
    private String hostUuid;
    private String vSwitchType;
    private boolean createChain = true;

    public void setSdnControllerUuid(String sdnControllerUuid) {
        this.sdnControllerUuid = sdnControllerUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getvSwitchType() {
        return vSwitchType;
    }

    public void setvSwitchType(String vSwitchType) {
        this.vSwitchType = vSwitchType;
    }

    @Override
    public String getSdnControllerUuid() {
        return sdnControllerUuid;
    }

    public boolean isCreateChain() {
        return createChain;
    }

    public void setCreateChain(boolean createChain) {
        this.createChain = createChain;
    }
}
