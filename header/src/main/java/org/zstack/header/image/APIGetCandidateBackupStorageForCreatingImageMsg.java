package org.zstack.header.image;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.volume.VolumeVO;

/**
 * Created by xing5 on 2016/8/30.
 */
@Action(category = ImageConstant.ACTION_CATEGORY, names = {"read"})
public class APIGetCandidateBackupStorageForCreatingImageMsg extends APISyncCallMessage {
    @APIParam(resourceType = VolumeVO.class, required = false)
    private String volumeUuid;
    @APIParam(resourceType = VolumeSnapshotVO.class, required = false)
    private String volumeSnapshotUuid;

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getVolumeSnapshotUuid() {
        return volumeSnapshotUuid;
    }

    public void setVolumeSnapshotUuid(String volumeSnapshotUuid) {
        this.volumeSnapshotUuid = volumeSnapshotUuid;
    }
}
