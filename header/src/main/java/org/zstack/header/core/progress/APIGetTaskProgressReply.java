package org.zstack.header.core.progress;

import org.zstack.header.message.APIReply;

import java.sql.Timestamp;
/**
 * Created by mingjian.deng on 16/12/8.
 */
public class APIGetTaskProgressReply extends APIReply {private String progress;
    private String resourceUuid;
    private String processType;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public String getProgress() {
        return progress;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

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

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
