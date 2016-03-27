package org.zstack.storage.primary.smp;

import org.zstack.header.storage.primary.APIAddPrimaryStorageMsg;

/**
 * Created by xing5 on 2016/3/27.
 */
public class APIAddSharedMountPointPrimaryStorageMsg extends APIAddPrimaryStorageMsg {
    public APIAddSharedMountPointPrimaryStorageMsg() {
        this.setType(SMPConstants.SMP_TYPE);
    }

    @Override
    public String getType() {
        return SMPConstants.SMP_TYPE;
    }
}
