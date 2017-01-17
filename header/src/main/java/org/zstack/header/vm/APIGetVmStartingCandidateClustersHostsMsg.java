package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2016/5/14.
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/vm-instances/{uuid}/starting-target-hosts",
        method = HttpMethod.GET,
        responseClass = APIGetVmStartingCandidateClustersHostsReply.class
)
public class APIGetVmStartingCandidateClustersHostsMsg extends APISyncCallMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }
 
    public static APIGetVmStartingCandidateClustersHostsMsg __example__() {
        APIGetVmStartingCandidateClustersHostsMsg msg = new APIGetVmStartingCandidateClustersHostsMsg();


        return msg;
    }

}
