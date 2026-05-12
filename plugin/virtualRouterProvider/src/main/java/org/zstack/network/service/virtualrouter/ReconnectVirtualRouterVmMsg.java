package org.zstack.network.service.virtualrouter;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceMessage;

/**
 * Created by frank on 6/29/2015.
 */
public class ReconnectVirtualRouterVmMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String virtualRouterVmUuid;
    private boolean statusChange = false;
    private boolean skipGrayscaleUpgradeCheck = false;


    public String getVirtualRouterVmUuid() {
        return virtualRouterVmUuid;
    }

    public void setVirtualRouterVmUuid(String virtualRouterVmUuid) {
        this.virtualRouterVmUuid = virtualRouterVmUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return virtualRouterVmUuid;
    }

    public boolean isStatusChange() {
        return statusChange;
    }

    public void setStatusChange(boolean statusChange) {
        this.statusChange = statusChange;
    }

    public boolean isSkipGrayscaleUpgradeCheck() {
        return skipGrayscaleUpgradeCheck;
    }

    public void setSkipGrayscaleUpgradeCheck(boolean skipGrayscaleUpgradeCheck) {
        this.skipGrayscaleUpgradeCheck = skipGrayscaleUpgradeCheck;
    }
}
