package org.zstack.header.storage.primary;

import org.zstack.header.message.APISyncCallMessage;

/**
 * @api
 *
 * get supported primary storage type
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 *{
"org.zstack.header.storage.primary.APIGetPrimaryStorageTypesMsg": {
"session": {
"uuid": "a096426cb6c64ede865cf9577f745906"
}
}
}
 * @msg
 *
 * {
"org.zstack.header.storage.primary.APIGetPrimaryStorageTypesMsg": {
"session": {
"uuid": "a096426cb6c64ede865cf9577f745906"
},
"timeout": 1800000,
"id": "7ff3b617bd534634937beb8763d2ed92",
"serviceId": "api.portal"
}
}
 *
 * @result
 *
 * see :ref:`APIGetPrimaryStorageTypesReply`
 */
public class APIGetPrimaryStorageTypesMsg extends APISyncCallMessage{
}
