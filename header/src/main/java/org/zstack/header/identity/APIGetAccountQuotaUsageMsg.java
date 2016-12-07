package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 2/22/2016.
 */
@Action(category = AccountConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/accounts/quota/{uuid}/usages",
        method = HttpMethod.GET,
        parameterName = "params",
        responseClass = APIGetAccountQuotaUsageReply.class
)
public class APIGetAccountQuotaUsageMsg extends APISyncCallMessage implements AccountMessage {
    @APIParam(resourceType = AccountVO.class, checkAccount = true, operationTarget = true, required = false)
    private String uuid;

    @Override
    public String getAccountUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
