package org.zstack.header.storage.snapshot;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.vm.VmInstanceVO;

/**
 * Created by miao on 11/3/16.
 */
@Action(category = VolumeSnapshotConstant.ACTION_CATEGORY)
public class APIReimageVmInstanceMsg extends APIMessage {
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String vmInstanceUuid;
}
