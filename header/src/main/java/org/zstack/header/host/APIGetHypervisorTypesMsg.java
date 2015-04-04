package org.zstack.header.host;

import org.zstack.header.message.APISyncCallMessage;

/**
 * @api
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.header.host.APIGetHypervisorTypesMsg": {
"session": {
"uuid": "c58ec5b783ea458a8c2234c5130b7299"
}
}
}
 *
 * @msg
 * {
"org.zstack.header.host.APIGetHypervisorTypesMsg": {
"session": {
"uuid": "c58ec5b783ea458a8c2234c5130b7299"
},
"timeout": 1800000,
"serviceId": "api.portal"
}
}
 *
 * @result
 *
 * see :ref:`APIGetHypervisorTypesReply`
 */
public class APIGetHypervisorTypesMsg extends APISyncCallMessage {
}
