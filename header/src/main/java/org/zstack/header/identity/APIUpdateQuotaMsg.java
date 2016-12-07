package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 7/14/2015.
 */
@RestRequest(
        path = "/accounts/quotas/actions",
        responseClass = APIUpdateQuotaEvent.class,
        isAction = true,
        method = HttpMethod.PUT
)
public class APIUpdateQuotaMsg extends APIMessage implements AccountMessage {
    @APIParam(resourceType = AccountVO.class)
    private String identityUuid;
    @APIParam
    private String name;
    @APIParam
    private long value;

    @Override
    public String getAccountUuid() {
        return getSession().getAccountUuid();
    }

    public String getIdentityUuid() {
        return identityUuid;
    }

    public void setIdentityUuid(String identityUuid) {
        this.identityUuid = identityUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }
}
