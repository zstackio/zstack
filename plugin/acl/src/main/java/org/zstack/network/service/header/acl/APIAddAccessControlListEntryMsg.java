package org.zstack.network.service.header.acl;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-09
 **/
@TagResourceType(AccessControlListEntryVO.class)
@Action(category = AccessControlListConstants.ACTION_CATEGORY)
@RestRequest(
        path = "/access-control-lists/{aclUuid}/ipentries",
        method = HttpMethod.POST,
        responseClass = APIAddAccessControlListEntryEvent.class,
        parameterName = "params"
)
public class APIAddAccessControlListEntryMsg extends APICreateMessage implements APIAuditor {
    @APIParam(resourceType = AccessControlListVO.class, checkAccount = true, operationTarget = true)
    private String aclUuid;
    @APIParam(maxLength = 2048)
    private String entries;
    @APIParam(maxLength = 2048, required = false)
    private String description;

    public String getAclUuid() {
        return aclUuid;
    }

    public void setAclUuid(String aclUuid) {
        this.aclUuid = aclUuid;
    }

    public String getEntries() {
        return entries;
    }

    public void setEntries(String entries) {
        this.entries = entries;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static APIAddAccessControlListEntryMsg __example__() {
        APIAddAccessControlListEntryMsg msg = new APIAddAccessControlListEntryMsg();

        msg.setAclUuid(uuid());
        msg.setEntries("192.168.12.1,192.168.48.0/24");

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? String.valueOf(((APIAddAccessControlListEntryEvent)rsp).getInventory().getEntryId()) : "", AccessControlListEntryVO.class);
    }
}
