package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/3/6 13:42
 */
public class KvmReportVmShutdownEventMsg extends NeedReplyMessage implements VmInstanceMessage{
    private String vmInstanceUuid;

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }
}
