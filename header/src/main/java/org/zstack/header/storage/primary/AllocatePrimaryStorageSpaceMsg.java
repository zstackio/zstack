package org.zstack.header.storage.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.utils.CollectionDSL;

import java.util.ArrayList;
import java.util.List;

public class AllocatePrimaryStorageSpaceMsg extends AllocatePrimaryStorageMsg{
    private String requiredAllocatedInstallUrl;

    public String getRequiredAllocatedInstallUrl() {
        return requiredAllocatedInstallUrl;
    }

    public void setRequiredAllocatedInstallUrl(String requiredAllocatedInstallUrl) {
        this.requiredAllocatedInstallUrl = requiredAllocatedInstallUrl;
    }
}
