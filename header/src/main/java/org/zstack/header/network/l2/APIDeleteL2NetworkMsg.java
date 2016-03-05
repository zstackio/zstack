package org.zstack.header.network.l2;

import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;

/**
 * @api
 *
 * delete l2Network
 *
 * @category l2Network
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.header.network.l2.APIDeleteL2NetworkMsg": {
"l2NetworkUuid": "ce3602cff6484ca09765ee3dd2af81c2",
"deleteMode": "Permissive",
"session": {
"uuid": "1d8cfba780314ab498bfcdda72357527"
}
}
}
 * @msg
 * {
"org.zstack.header.network.l2.APIDeleteL2NetworkMsg": {
"l2NetworkUuid": "ce3602cff6484ca09765ee3dd2af81c2",
"deleteMode": "Permissive",
"session": {
"uuid": "1d8cfba780314ab498bfcdda72357527"
},
"timeout": 1800000,
"id": "eeec6b87abbf4325851e1751e8c87fb2",
"serviceId": "api.portal"
}
}
 * @result
 * see :ref:`APIDeleteL2NetworkEvent`
 */
public class APIDeleteL2NetworkMsg extends APIDeleteMessage implements L2NetworkMessage {
    /**
     * @desc l2Network uuid
     */
    @APIParam
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getL2NetworkUuid() {
        return uuid;
    }
}
