package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

@Action(category = AccountConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/accesskey",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APICreateAccessKeyEvent.class
)
public class APICreateAccessKeyMsg extends APICreateMessage implements APIAuditor {
    @APIParam(resourceType = AccountVO.class, checkAccount = true)
    private String accountUuid;
    @APIParam(maxLength = 32)
    private String userUuid;
    @APIParam(maxLength = 2048, required = false)
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    public static APICreateAccessKeyMsg __example__() {
        APICreateAccessKeyMsg msg = new APICreateAccessKeyMsg();
        msg.setAccountUuid(uuid());
        msg.setUserUuid(uuid());
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("Creating").resource(((APICreateAccessKeyEvent) evt).getInventory().getUuid(), AccountAccessKeyVO.class.getSimpleName())
                            .messageAndEvent(that, evt).done();
                }
            }
        };
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        String uuid = "";
        if (rsp.isSuccess()) {
            APICreateAccessKeyEvent evt = (APICreateAccessKeyEvent) rsp;
            uuid = evt.getInventory().getUuid();
        }
        return new Result(uuid, AccountAccessKeyVO.class);
    }
}
