package org.zstack.hotfix;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.storage.primary.PrimaryStorageVO;

/**
 * Created by xing5 on 2016/10/25.
 */
public class APIHotFix1169KvmSnapshotChainMsg extends APIMessage {
    @APIParam(resourceType = PrimaryStorageVO.class)
    private String primaryStorageUuid;

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
}
