package org.zstack.header.core.progress;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;

/**
 * Created by mingjian.deng on 16/12/8.
 */
@Action(category = ProgressConstants.ACTION_CATEGORY)
public class APIGetTaskProgressMsg extends APISyncCallMessage {
    @APIParam
    private String resourceUuid;

    @APIParam(required = false, validValues = {"AddImage", "LocalStorageMigrateVolume",
            "CreateRootVolumeTemplateFromRootVolume"})
    private String processType;

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getProcessType() {
        return processType;
    }

    public void setProcessType(String processType) {
        this.processType = processType;
    }
}
