package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/4/12 17:05
 */
public class KvmReportVmShutdownFromGuestEventMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }
}
