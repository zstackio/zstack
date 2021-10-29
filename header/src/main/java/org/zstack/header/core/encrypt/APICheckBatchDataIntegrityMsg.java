package org.zstack.header.core.encrypt;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

import java.util.Arrays;
import java.util.List;

/**
 * @Author: DaoDao
 * @Date: 2021/11/12
 */
@RestRequest(
        path = "/check/Batch/data/integrity/",
        method = HttpMethod.GET,
        responseClass = APICheckBatchDataIntegrityReply.class
)
public class APICheckBatchDataIntegrityMsg extends APISyncCallMessage {
    private List<String> resourceUuids;

    private String resourceType;

    public List<String> getResourceUuids() {
        return resourceUuids;
    }

    public void setResourceUuids(List<String> resourceUuids) {
        this.resourceUuids = resourceUuids;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public static APICheckBatchDataIntegrityMsg __example__() {
        APICheckBatchDataIntegrityMsg msg = new APICheckBatchDataIntegrityMsg();
        msg.resourceUuids = Arrays.asList(uuid());
        msg.resourceType = "GlobalConfigVO";
        return msg;
    }

}
