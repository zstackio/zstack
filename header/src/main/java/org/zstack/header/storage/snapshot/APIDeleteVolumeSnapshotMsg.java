package org.zstack.header.storage.snapshot;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;

/**
 * @api
 *
 * delete a volume snapshot from primary storage and all its copies from backup stroage
 *
 * @category volume snapshot
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.header.storage.snapshot.APIDeleteVolumeSnapshotMsg": {
"uuid": "a4efe4e48c424d009cffe2faae018c70",
"deleteMode": "Permissive",
"session": {
"uuid": "d7d1fb6650d64b20b2b050b0eb06448b"
}
}
}
 * @msg
 *
 * {
"org.zstack.header.storage.snapshot.APIDeleteVolumeSnapshotMsg": {
"uuid": "a4efe4e48c424d009cffe2faae018c70",
"deleteMode": "Permissive",
"session": {
"uuid": "d7d1fb6650d64b20b2b050b0eb06448b"
},
"timeout": 1800000,
"id": "eae63912be604948a2fc19fe1ff907a6",
"serviceId": "api.portal"
}
}
 *
 * @result
 *
 * see :ref:`APIDeleteVolumeSnapshotEvent`
 */
@Action(category = VolumeSnapshotConstant.ACTION_CATEGORY)
public class APIDeleteVolumeSnapshotMsg extends APIDeleteMessage implements VolumeSnapshotMessage {
    /**
     * @desc volume snapshot uuid
     */
    @APIParam(checkAccount = true, operationTarget = true)
    private String uuid;

    /**
     * @ignore
     */
    @APINoSee
    private String volumeUuid;
    /**
     * @ignore
     */
    @APINoSee
    private String treeUuid;

    @Override
    public String getTreeUuid() {
        return treeUuid;
    }

    @Override
    public void setTreeUuid(String treeUuid) {
        this.treeUuid = treeUuid;
    }
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getSnapshotUuid() {
        return uuid;
    }

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}
