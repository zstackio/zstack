package org.zstack.header.vm;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;

/**
 * @api
 *
 * destroy a vm instance
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 *
 * {
"org.zstack.header.vm.APIDestroyVmInstanceMsg": {
"uuid": "a3047245c27c49a08943f2dd37e65a63",
"deleteMode": "Permissive",
"session": {
"uuid": "616b27b659dd496a9a98cd62348bfef0"
}
}
}
 *
 * @msg
 *
 * {
"org.zstack.header.vm.APIDestroyVmInstanceMsg": {
"uuid": "a3047245c27c49a08943f2dd37e65a63",
"deleteMode": "Permissive",
"session": {
"uuid": "616b27b659dd496a9a98cd62348bfef0"
},
"timeout": 1800000,
"id": "cced165a85fe4fcbb81d55b27695a6ea",
"serviceId": "api.portal"
}
}
 *
 * @result
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
public class APIDestroyVmInstanceMsg extends APIDeleteMessage implements VmInstanceMessage {
    /**
     * @desc vm uuid
     */
    @APIParam(checkAccount = true, operationTarget = true)
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
