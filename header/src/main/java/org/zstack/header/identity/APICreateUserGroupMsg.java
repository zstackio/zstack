package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

@Action(category = AccountConstant.ACTION_CATEGORY, accountOnly = true)
@RestRequest(
        path = "/accounts/groups",
        method = HttpMethod.POST,
        responseClass = APICreateUserGroupEvent.class,
        parameterName = "params"
)
public class APICreateUserGroupMsg extends APICreateMessage implements AccountMessage, APIAuditor {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;

    @Override
    public String getAccountUuid() {
        return this.getSession().getAccountUuid();
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
 
    public static APICreateUserGroupMsg __example__() {
        APICreateUserGroupMsg msg = new APICreateUserGroupMsg();

        msg.setName("usergroup");

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateUserGroupEvent)rsp).getInventory().getUuid() : "", UserGroupVO.class);
    }
}
