package org.zstack.header.volume;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.volume.VolumeMessage;
import org.zstack.header.volume.VolumeVO;

/**
 * @api
 *
 * detach a data volume from vm
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 *
 * {
"org.zstack.header.volume.APIDetachDataVolumeMsg": {
"volumeUuid": "57e060d2eb324da4bc65c1c5ad9a6e59",
"session": {
"uuid": "9f1fddca86544ad282f4c3ffe12d5f10"
}
}
}
 *
 * @msg
 *
 * {
"org.zstack.header.volume.APIDetachDataVolumeMsg": {
"volumeUuid": "57e060d2eb324da4bc65c1c5ad9a6e59",
"session": {
"uuid": "9f1fddca86544ad282f4c3ffe12d5f10"
},
"timeout": 1800000,
"id": "d32847e788d145648cf0e53994d451e2",
"serviceId": "api.portal"
}
}
 *
 * @result
 *
 * See :ref:`APIDetachDataVolumeEvent`
 */
@Action(category = VolumeConstant.ACTION_CATEGORY)
public class APIDetachDataVolumeFromVmMsg extends APIMessage implements VolumeMessage {
    /**
     * @desc data volume uuid. See :ref:`VolumeInventory`
     */
    @APIParam(resourceType = VolumeVO.class, checkAccount = true, operationTarget = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    @Override
    public String getVolumeUuid() {
        return uuid;
    }

    public void setUuid(String volumeUuid) {
        this.uuid = volumeUuid;
    }
}
