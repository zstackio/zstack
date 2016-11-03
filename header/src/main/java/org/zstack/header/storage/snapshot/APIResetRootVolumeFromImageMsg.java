package org.zstack.header.storage.snapshot;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.volume.VolumeVO;

/**
 * Created by miao on 11/3/16.
 */
@Action(category = VolumeSnapshotConstant.ACTION_CATEGORY)
public class APIResetRootVolumeFromImageMsg extends APIMessage {
    public String getRootVolumeUuid() {
        return rootVolumeUuid;
    }

    public void setRootVolumeUuid(String rootVolumeUuid) {
        this.rootVolumeUuid = rootVolumeUuid;
    }

    @APIParam(resourceType = VolumeVO.class, checkAccount = true, operationTarget = true)
    private String rootVolumeUuid;
}
