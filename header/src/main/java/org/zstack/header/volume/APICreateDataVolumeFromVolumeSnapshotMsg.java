package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;

/**
 * @api create data volume from a volume snapshot
 * @category volume
 * @cli
 * @httpMsg {
 * "org.zstack.header.volume.APICreateDataVolumeFromVolumeSnapshotMsg": {
 * "name": "volume-form-snapshotb86f375d5ebf455b8037021f8e641fc8",
 * "volumeSnapshotUuid": "b86f375d5ebf455b8037021f8e641fc8",
 * "session": {
 * "uuid": "cb3ffca02e214c10a0ed8b8bb54cdf97"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.volume.APICreateDataVolumeFromVolumeSnapshotMsg": {
 * "name": "volume-form-snapshotb86f375d5ebf455b8037021f8e641fc8",
 * "volumeSnapshotUuid": "b86f375d5ebf455b8037021f8e641fc8",
 * "session": {
 * "uuid": "cb3ffca02e214c10a0ed8b8bb54cdf97"
 * },
 * "timeout": 1800000,
 * "id": "2d39666ae00a4ffebb231e5bd1e1d285",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APICreateDataVolumeFromVolumeSnapshotEvent`
 * @since 0.1.0
 */
@Action(category = VolumeConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/volumes/data/from/volume-snapshots/{volumeSnapshotUuid}",
        responseClass = APICreateDataVolumeFromVolumeSnapshotEvent.class,
        method = HttpMethod.POST,
        parameterName = "params"
)
public class APICreateDataVolumeFromVolumeSnapshotMsg extends APICreateMessage {
    /**
     * @desc max length of 255 characters
     */
    @APIParam(maxLength = 255)
    private String name;
    /**
     * @desc max length of 2048 characters
     */
    @APIParam(required = false, maxLength = 2048)
    private String description;
    /**
     * @desc volume snapshot uuid
     */
    @APIParam(resourceType = VolumeSnapshotVO.class, checkAccount = true, operationTarget = true)
    private String volumeSnapshotUuid;
    /**
     * @desc uuid of primary storage where the data volume is being created. If omitted,
     * zstack will try find a proper one
     * @optional
     */
    @APIParam(required = false, resourceType = PrimaryStorageVO.class)
    private String primaryStorageUuid;

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

    public String getVolumeSnapshotUuid() {
        return volumeSnapshotUuid;
    }

    public void setVolumeSnapshotUuid(String volumeSnapshotUuid) {
        this.volumeSnapshotUuid = volumeSnapshotUuid;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
}
