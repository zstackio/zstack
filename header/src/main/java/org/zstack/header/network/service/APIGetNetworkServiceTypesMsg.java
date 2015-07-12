package org.zstack.header.network.service;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l3.L3NetworkConstant;

/**
 * @api
 *
 * get supported network service types
 *
 * @category network service
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.header.network.service.APIGetNetworkServiceTypesMsg": {
"session": {
"uuid": "acc010d17fa64ab0bf86fce209b67753"
}
}
}
 * @msg
 * {
"org.zstack.header.network.service.APIGetNetworkServiceTypesMsg": {
"session": {
"uuid": "acc010d17fa64ab0bf86fce209b67753"
},
"timeout": 1800000,
"id": "1819bb8648264fe79a03a6953108ae6e",
"serviceId": "api.portal"
}
}
 *
 * @result
 * see :ref:`APIGetNetworkServiceTypesReply`
 */
@Action(category = L3NetworkConstant.ACTION_CATEGORY, names = {"read"})
public class APIGetNetworkServiceTypesMsg extends APISyncCallMessage {
}
