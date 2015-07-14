package org.zstack.header.network.l3;

import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

/**
 * @api
 * change l3Network state
 *
 * @category l3Network
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.header.network.l3.APIChangeL3NetworkStateMsg": {
"uuid": "3424b7a643c348c795aaa8df59c5044f",
"stateEvent": "enable",
"session": {
"uuid": "6d0f7c288ae645458390b5f79ebbc012"
}
}
}
 * @msg
 * {
"org.zstack.header.network.l3.APIChangeL3NetworkStateMsg": {
"uuid": "3424b7a643c348c795aaa8df59c5044f",
"stateEvent": "enable",
"session": {
"uuid": "6d0f7c288ae645458390b5f79ebbc012"
},
"timeout": 1800000,
"id": "551532e755484338a704bf5fba980a2d",
"serviceId": "api.portal"
}
}
 *
 * @result
 *
 * see :ref:`APIChangeL3NetworkStateEvent`
 */
@Action(category = L3NetworkConstant.ACTION_CATEGORY)
public class APIChangeL3NetworkStateMsg extends APIMessage implements L3NetworkMessage {
    /**
     * @desc
     * l3Network uuid
     */
    @APIParam(resourceType = L3NetworkVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    /**
     * @desc
     * - enable: enable l3Network
     * - disable: disable l3Network
     *
     * for details of state of l3Network, see state of :ref:`L3NetworkInventory`
     * @choices
     * - enable
     * - disable
     */
    @APIParam(validValues = {"enable", "disable"})
    private String stateEvent;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getStateEvent() {
        return stateEvent;
    }

    public void setStateEvent(String stateEvent) {
        this.stateEvent = stateEvent;
    }

    @Override
    public String getL3NetworkUuid() {
        return uuid;
    }
}
