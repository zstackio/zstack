package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2016/9/20.
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/images/iso/{isoUuid}/vm-candidates",
        method = HttpMethod.GET,
        responseClass = APIGetCandidateVmForAttachingIsoReply.class,
        parameterName = "null"
)
public class APIGetCandidateVmForAttachingIsoMsg extends APISyncCallMessage {
    @APIParam(resourceType = ImageVO.class)
    private String isoUuid;

    public String getIsoUuid() {
        return isoUuid;
    }

    public void setIsoUuid(String isoUuid) {
        this.isoUuid = isoUuid;
    }
 
    public static APIGetCandidateVmForAttachingIsoMsg __example__() {
        APIGetCandidateVmForAttachingIsoMsg msg = new APIGetCandidateVmForAttachingIsoMsg();


        return msg;
    }

}
