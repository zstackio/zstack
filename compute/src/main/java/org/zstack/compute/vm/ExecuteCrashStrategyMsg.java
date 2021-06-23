package org.zstack.compute.vm;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.SkipVmTracer;
import org.zstack.header.vm.VmInstanceMessage;

/**
 * Created by LiangHanYu on 2021/6/22 15:34
 */
@SkipVmTracer(replyClass = ExecuteCrashStrategyReply.class)
public class ExecuteCrashStrategyMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;
    private boolean needToReboot = true;

    public boolean getNeedToReboot() {
        return needToReboot;
    }

    public void setNeedToReboot(boolean needToReboot) {
        this.needToReboot = needToReboot;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }
}
