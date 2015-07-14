package org.zstack.header.vm;

import org.zstack.header.host.HostVO;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
/**
 * @api
 *
 * live migrate vm to another host
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 *
 * {
"org.zstack.header.vm.APIMigrateVmMsg": {
"vmInstanceUuid": "7fed8055cc004d8fbafd2918efdb2fcc",
"hostUuid": "4796eba438884c49b6d754b33f84d582",
"session": {
"uuid": "f16d128c42eb4e90b0490499c4e51e67"
}
}
}
 *
 * @msg
 * {
"org.zstack.header.vm.APIMigrateVmMsg": {
"vmInstanceUuid": "7fed8055cc004d8fbafd2918efdb2fcc",
"hostUuid": "4796eba438884c49b6d754b33f84d582",
"session": {
"uuid": "f16d128c42eb4e90b0490499c4e51e67"
},
"timeout": 1800000,
"id": "9fab8a857fe941beaaef2d5d2c30cb50",
"serviceId": "api.portal"
}
}
 *
 * @result
 *
 * See :ref:`APIMigrateVmEvent`
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
public class APIMigrateVmMsg extends APIMessage implements VmInstanceMessage {
    /**
     * @desc vm uuid
     */
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String vmInstanceUuid;
    /**
     * @desc uuid of host the vm is migrating to. If omitted, zstack will try to find
     * an available host itself
     * @optional
     */
    @APIParam(required = false, resourceType = HostVO.class)
    private String hostUuid;
    public String getVmUuid() {
        return vmInstanceUuid;
    }
    public void setVmUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
    public String getHostUuid() {
        return hostUuid;
    }
    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
    @Override
    public String getVmInstanceUuid() {
        return getVmUuid();
    }
}
