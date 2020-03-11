package org.zstack.network.service.header.acl;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-09
 **/
@Action(category = AccessControlListConstants.ACTION_CATEGORY)
@RestRequest(
        path = "/access-control-lists/{aclUuid}/ipentries/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveAccessControlListEntryEvent.class
)
public class APIRemoveAccessControlListEntryMsg extends APIDeleteMessage implements APIAuditor{
    @APIParam(resourceType = AccessControlListVO.class, checkAccount = true, operationTarget = true)
    private String aclUuid;
    @APIParam(resourceType = AccessControlListEntryVO.class, checkAccount = true, operationTarget = true, successIfResourceNotExisting = true)
    private Long entryId;

    public  String getAclUuid() {
        return aclUuid;
    }

    public void setAclUuid(String aclUuid) {
        this.aclUuid = aclUuid;
    }

    public Long getEntryId() {
        return entryId;
    }

    public void setEntryId(Long entryId) {
        this.entryId = entryId;
    }

    public static APIRemoveAccessControlListEntryMsg __example__() {
        APIRemoveAccessControlListEntryMsg msg = new APIRemoveAccessControlListEntryMsg();
        msg.setEntryId(123L);
        return msg;
    }

    @Override
    public APIAuditor.Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(String.valueOf(((APIRemoveAccessControlListEntryMsg)msg).getEntryId()), AccessControlListEntryVO.class);
    }
}
