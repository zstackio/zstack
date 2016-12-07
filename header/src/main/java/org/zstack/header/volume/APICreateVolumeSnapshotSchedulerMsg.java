package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.core.scheduler.APICreateSchedulerMessage;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by root on 7/11/16.
 */
@Action(category = VolumeConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/volumes/schedulers/create-volume-snapshots",
        method = HttpMethod.POST,
        responseClass = APICreateVolumeSnapshotSchedulerEvent.class,
        parameterName = "params"
)
public class APICreateVolumeSnapshotSchedulerMsg extends APICreateSchedulerMessage implements VolumeMessage {
    /**
     * @desc volume uuid. See :ref:`VolumeInventory`
     */
    @APIParam(resourceType = VolumeVO.class, checkAccount = true, operationTarget = true)
    private String volumeUuid;
    /**
     * @desc snapshot name. Max length of 255 characters
     */
    @APIParam(maxLength = 255)
    private String snapShotName;
    /**
     * @desc snapshot description. Max length of 2048 characters
     */
    @APIParam(required = false, maxLength = 2048)
    private String volumeSnapshotDescription;

    @Override
    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getSnapShotName() {
        return snapShotName;
    }

    public void setSnapShotName(String snapShotName) {
        this.snapShotName = snapShotName;
    }

    public String getVolumeSnapshotDescription() {
        return volumeSnapshotDescription;
    }

    public void setVolumeSnapshotDescription(String volumeSnapshotDescription) {
        this.volumeSnapshotDescription = volumeSnapshotDescription;
    }

}
