package org.zstack.header.acl;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

/**
 * Created by boce.wang on 05/13/2025.
 */

@Action(category = AccessControlListConstants.ACTION_CATEGORY)
@RestRequest(
        path = "/access-control-lists/{uuid}/actions",
        method = "PUT",
        responseClass = APIUpdateAccessControlListEvent.class,
        isAction = true
)
public class APIUpdateAccessControlListMsg extends APIMessage implements APIAuditor {
    @APIParam(resourceType = AccessControlListVO.class, checkAccount = true)
    private String uuid;
    @APIParam(required = false, maxLength = 255, emptyString = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIUpdateAccessControlListEvent)rsp).getInventory().getUuid() : "", AccessControlListVO.class);
    }

    public static APIUpdateAccessControlListMsg __example__() {
        APIUpdateAccessControlListMsg msg = new APIUpdateAccessControlListMsg();
        msg.setName("acl-1");
        msg.setDescription("acl-1 description");
        return msg;
    }
}
