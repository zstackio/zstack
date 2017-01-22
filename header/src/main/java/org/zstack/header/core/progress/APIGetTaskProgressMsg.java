package org.zstack.header.core.progress;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 16/12/8.
 */
@Action(category = ProgressConstants.ACTION_CATEGORY)
@RestRequest(
        path = "/progress/{processType}/{resourceUuid}",
        responseClass = APIGetTaskProgressReply.class,
        method = HttpMethod.GET
)
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
 
    public static APIGetTaskProgressMsg __example__() {
        APIGetTaskProgressMsg msg = new APIGetTaskProgressMsg();
        msg.setResourceUuid("f16661c706ae403883f5e4cca6f1f3f4");
        msg.setProcessType("AddImage");

        return msg;
    }

}
