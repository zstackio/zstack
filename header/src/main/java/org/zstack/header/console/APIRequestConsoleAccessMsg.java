package org.zstack.header.console;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.vm.VmInstance;
import org.zstack.header.vm.VmInstanceVO;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class APIRequestConsoleAccessMsg extends APIMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
