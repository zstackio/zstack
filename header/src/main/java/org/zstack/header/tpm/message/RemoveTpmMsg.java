package org.zstack.header.tpm.message;

import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.DeletionMessage;
import org.zstack.header.tpm.api.APIRemoveTpmMsg;

public class RemoveTpmMsg extends DeletionMessage {
    private String tpmUuid;
    private String vmInstanceUuid;

    public String getTpmUuid() {
        return tpmUuid;
    }

    public void setTpmUuid(String tpmUuid) {
        this.tpmUuid = tpmUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public static RemoveTpmMsg valueOf(APIRemoveTpmMsg api) {
        RemoveTpmMsg msg = new RemoveTpmMsg();
        msg.setTpmUuid(api.getTpmUuid());
        msg.setVmInstanceUuid(api.getVmInstanceUuid());
        msg.setForceDelete(api.getDeletionMode() == APIDeleteMessage.DeletionMode.Enforcing);
        return msg;
    }
}
