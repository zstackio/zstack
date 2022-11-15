package org.zstack.header.volume;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

@RestResponse(fieldsTo = "all")
public class APIBatchSyncVolumeSizeReply extends APIReply {
    private Integer successCount;

    private Integer failCount;

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setFailCount(Integer failCount) {
        this.failCount = failCount;
    }

    public Integer getFailCount() {
        return failCount;
    }
}
