package org.zstack.header.console;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.vm.VmInstanceVO;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:27 PM
 * To change this template use File | Settings | File Templates.
 */
@Action(category = ConsoleConstants.ACTION_CATEGORY)
public class APIRequestConsoleAccessMsg extends APIMessage {
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String vmInstanceUuid;

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
