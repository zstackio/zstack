package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * Created by xing5 on 2016/4/16.
 */
@RestRequest(
        path = "/account/{accountUuid}/resources",
        method = HttpMethod.POST,
        responseClass = APIChangeResourceOwnerEvent.class,
        parameterName = "params"
)
public class APIChangeResourceOwnerMsg extends APIMessage {
    @APIParam(resourceType = AccountVO.class)
    private String accountUuid;
    @APIParam
    private String resourceUuid;

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }
 
    public static APIChangeResourceOwnerMsg __example__() {
        APIChangeResourceOwnerMsg msg = new APIChangeResourceOwnerMsg();
        msg.setAccountUuid(uuid());
        msg.setResourceUuid(uuid());

        return msg;
    }
}
