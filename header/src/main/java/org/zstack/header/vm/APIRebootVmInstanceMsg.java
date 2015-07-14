package org.zstack.header.vm;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
/**
 * @api
 *
 * reboot a vm instance
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 *
 * {
"org.zstack.header.vm.APIRebootVmInstanceMsg": {
"uuid": "bfc19807cdde47d5ac18ef76ed0b450f",
"session": {
"uuid": "07682ae6cc4648d7975d0d2e9fe9127c"
}
}
}
 *
 * @msg
 *
 * {
"org.zstack.header.vm.APIRebootVmInstanceMsg": {
"uuid": "bfc19807cdde47d5ac18ef76ed0b450f",
"session": {
"uuid": "07682ae6cc4648d7975d0d2e9fe9127c"
},
"timeout": 1800000,
"id": "1e784af42f334997a9382b1e990b53c8",
"serviceId": "api.portal"
}
}
 *
 * @result
 *
 * see :ref:`APIRebootVmInstanceEvent`
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
public class APIRebootVmInstanceMsg extends APIMessage implements VmInstanceMessage {
    /**
     * @desc vm uuid
     */
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return getUuid();
    }
}
