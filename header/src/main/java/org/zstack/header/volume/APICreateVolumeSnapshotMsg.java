package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.snapshot.VolumeSnapshotConstant;

/**
 * @api create a volume snapshot from volume
 * @category volume snapshot
 * @cli
 * @httpMsg {
 * "org.zstack.header.volume.APICreateVolumeSnapshotMsg": {
 * "volumeUuid": "f899a58f1d0c426bad3a0cc3da4124f0",
 * "name": "Snapshot-f899a58f1d0c426bad3a0cc3da4124f0",
 * "description": "Test snapshot",
 * "session": {
 * "uuid": "7019b62326a848aa997b5a78c1cbf789"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.volume.APICreateVolumeSnapshotMsg": {
 * "volumeUuid": "f899a58f1d0c426bad3a0cc3da4124f0",
 * "name": "Snapshot-f899a58f1d0c426bad3a0cc3da4124f0",
 * "description": "Test snapshot",
 * "session": {
 * "uuid": "7019b62326a848aa997b5a78c1cbf789"
 * },
 * "timeout": 1800000,
 * "id": "84bd216529e64d4fb3bdf947da5585b3",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APICreateVolumeSnapshotEvent`
 * @since 0.1.0
 */
@Action(category = VolumeSnapshotConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/volumes/{volumeUuid}/volume-snapshots",
        method = HttpMethod.POST,
        responseClass = APICreateVolumeSnapshotEvent.class,
        parameterName = "params"
)
public class APICreateVolumeSnapshotMsg extends APICreateMessage implements VolumeMessage {
    /**
     * @desc volume uuid. See :ref:`VolumeInventory`
     */
    @APIParam(resourceType = VolumeVO.class, checkAccount = true, operationTarget = true)
    private String volumeUuid;
    /**
     * @desc snapshot name. Max length of 255 characters
     */
    @APIParam(maxLength = 255)
    private String name;
    /**
     * @desc snapshot description. Max length of 2048 characters
     */
    @APIParam(required = false, maxLength = 2048)
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}
