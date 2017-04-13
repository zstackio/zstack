package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
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
 
    public static APIUpdateQuotaMsg __example__() {
        APIUpdateQuotaMsg msg = new APIUpdateQuotaMsg();
        msg.setName("quotaname");
        msg.setIdentityUuid(uuid());
        msg.setValue(20);
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Updating a quota").resource(identityUuid, AccountVO.class.getSimpleName())
                        .context("quotaName", name)
                        .context("quotaValue", value)
                        .messageAndEvent(that, evt).done();
            }
        };
    }
}
