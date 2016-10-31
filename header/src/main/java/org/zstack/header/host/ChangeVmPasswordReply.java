package org.zstack.header.host;

import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmAccountPerference;

/**
 * Created by mingjian.deng on 16/10/19.
 */
public class ChangeVmPasswordReply extends MessageReply {
    private VmAccountPerference vmAccountPerference;

    public VmAccountPerference getVmAccountPerference() { return vmAccountPerference; }

    public void setVmAccountPerference(VmAccountPerference vmAccountPerference) { this.vmAccountPerference = vmAccountPerference; }
}
