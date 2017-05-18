package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 7/9/2015.
 */
@Action(category = AccountConstant.ACTION_CATEGORY, accountOnly = true)
@RestRequest(
        path = "/accounts/policies/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeletePolicyEvent.class
)
public class APIDeletePolicyMsg extends APIDeleteMessage implements AccountMessage {
    @APIParam(checkAccount = true, operationTarget = true)
    private String uuid;

    @Override
    public String getAccountUuid() {
        return getSession().getAccountUuid();
    }


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
 
    public static APIDeletePolicyMsg __example__() {
        APIDeletePolicyMsg msg = new APIDeletePolicyMsg();
        msg.setUuid(uuid());
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Deleting").resource(uuid, PolicyVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }
}
