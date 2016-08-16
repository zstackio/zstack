package org.zstack.header.vm;

import org.zstack.header.core.scheduler.APICreateSchedulerMessage;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;

/**
 * Created by root on 8/16/16.
 */

@Action(category = VmInstanceConstant.ACTION_CATEGORY)
public class APICreateRebootVmInstanceSchedulerMsg extends APICreateSchedulerMessage implements VmInstanceMessage {
    /**
     * @desc vm uuid
     */
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String vmUuid;

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return getVmUuid();
    }
}
