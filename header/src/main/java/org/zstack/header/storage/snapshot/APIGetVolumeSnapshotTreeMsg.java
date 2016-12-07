package org.zstack.header.storage.snapshot;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.volume.VolumeVO;

/**
 * @api get a volume snapshot tree for a volume
 * @category volume snapshot
 * @cli
 * @httpMsg {
 * "org.zstack.header.storage.snapshot.APIGetVolumeSnapshotTreeMsg": {
 * "treeUuid": "4c4fdfe0ec4b47528c23047b140ed577",
 * "session": {
 * "uuid": "d4d28025c13c4ffda1e7089ea1c6527f"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.storage.snapshot.APIGetVolumeSnapshotTreeMsg": {
 * "treeUuid": "4c4fdfe0ec4b47528c23047b140ed577",
 * "session": {
 * "uuid": "d4d28025c13c4ffda1e7089ea1c6527f"
 * },
 * "timeout": 1800000,
 * "id": "8f441550a001433991721aeb946609ee",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIGetVolumeSnapshotTreeReply`
 * @since 0.1.0
 */
public class APIGetVolumeSnapshotTreeMsg extends APISyncCallMessage {
    /**
     * @desc volume uuid. If set, all snapshot trees belonging to this volume will be returned. Could be null
     * @optional
     */
    @APIParam(required = false, resourceType = VolumeVO.class)
    private String volumeUuid;
    /**
     * @desc volume snapshot tree uuid. If set, only snapshot tree specified by this uuid will be returned. Could be null
     * <p>
     * .. note:: Either volumeUuid or treeUuid must be set. If both set, treeUuid takes preceding priority.
     * @optional
     */
    @APIParam(required = false, resourceType = VolumeSnapshotTreeVO.class)
    private String treeUuid;

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getTreeUuid() {
        return treeUuid;
    }

    public void setTreeUuid(String treeUuid) {
        this.treeUuid = treeUuid;
    }
}
