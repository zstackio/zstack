package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by xing5 on 2016/3/29.
 */
public class HaStartVmInstanceMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;
    private String judgerClassName;

    public String getJudgerClassName() {
        return judgerClassName;
    }

    public void setJudgerClassName(String judgerClassName) {
        this.judgerClassName = judgerClassName;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
