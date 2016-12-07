package org.zstack.header.storage.snapshot;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

/**
 * @api revert a volume to one of its snapshot
 * @category volume snapshot
 * @cli
 * @httpMsg {
 * "org.zstack.header.storage.snapshot.APIRevertVolumeFromSnapshotMsg": {
 * "uuid": "f163875e780249368b1e2b1e86ba7712",
 * "session": {
 * "uuid": "cb2dd066a3a24e8c99fea25e7816bde5"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.storage.snapshot.APIRevertVolumeFromSnapshotMsg": {
 * "uuid": "f163875e780249368b1e2b1e86ba7712",
 * "session": {
 * "uuid": "cb2dd066a3a24e8c99fea25e7816bde5"
 * },
 * "timeout": 1800000,
 * "id": "cffeac3ad46d4fffa4262ecc8aaaa699",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIRevertVolumeFromSnapshotEvent`
 * @since 0.1.0
 */

@Action(category = VolumeSnapshotConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/volume-snapshots/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIRevertVolumeFromSnapshotEvent.class
)
public class APIRevertVolumeFromSnapshotMsg extends APIMessage implements VolumeSnapshotMessage {
    /**
     * @desc volume snapshot uuid
     * <p>
     * .. note:: volume uuid is retrieved from snapshot
     */
    @APIParam(resourceType = VolumeSnapshotVO.class, checkAccount = true, operationTarget = true)
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

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
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
}
