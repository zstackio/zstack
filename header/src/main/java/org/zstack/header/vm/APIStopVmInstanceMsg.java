package org.zstack.header.vm;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
/**
 * @api
 *
 * stop a vm instance
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 *
{
"org.zstack.header.vm.APIStopVmInstanceMsg": {
"uuid": "b560cdf534404d86a8db76f3a2302213",
"session": {
"uuid": "7156d151bb0f461db6a3107d45c29929"
}
}
}
 *
 * @msg
 *{
"org.zstack.header.vm.APIStopVmInstanceMsg": {
"uuid": "b560cdf534404d86a8db76f3a2302213",
"session": {
"uuid": "7156d151bb0f461db6a3107d45c29929"
},
"timeout": 1800000,
"id": "4d4625ae532847d7a2c96adea53d2313",
"serviceId": "api.portal"
}
}
 * @result
 *
 * See :ref:`APIStopVmInstanceEvent`
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
public class APIStopVmInstanceMsg extends APIMessage implements VmInstanceMessage {
    /**
     * @desc vm uuid
     */
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam(required = false)
    private boolean force = false;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean getForce(){
        return force;
    }
    public void setForce(boolean force){
        this.force = force;
    }


    @Override
    public String getVmInstanceUuid() {
        return getUuid();
    }
}
