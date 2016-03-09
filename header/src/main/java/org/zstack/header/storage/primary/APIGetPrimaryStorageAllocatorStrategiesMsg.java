package org.zstack.header.storage.primary;

import org.zstack.header.message.APISyncCallMessage;

/**
 * @api
 * get allocation strategy of primary storage
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 *
{
"org.zstack.header.storage.primary.APIGetPrimaryStorageAllocatorStrategiesMsg": {
"session": {
"uuid": "b58fd452adeb4a4daefe02f8522af196"
}
}
}
 * @msg
 *{
"org.zstack.header.storage.primary.APIGetPrimaryStorageAllocatorStrategiesMsg": {
"session": {
"uuid": "b58fd452adeb4a4daefe02f8522af196"
},
"timeout": 1800000,
"id": "7d7685e74c5949d3afb955ec217da9fb",
"serviceId": "api.portal"
}
}
 * @result
 *
 * see :ref:`APIGetPrimaryStorageAllocatorStrategiesReply`
 */
public class APIGetPrimaryStorageAllocatorStrategiesMsg extends APISyncCallMessage {
}
