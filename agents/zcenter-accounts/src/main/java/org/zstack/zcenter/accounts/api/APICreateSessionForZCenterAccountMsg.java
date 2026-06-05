package org.zstack.zcenter.accounts.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.AccountSource;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
    path = "/zcenter/accounts/sessions",
    method = HttpMethod.POST,
    parameterName = "params",
    responseClass = APICreateSessionForZCenterAccountEvent.class
)
public class APICreateSessionForZCenterAccountMsg extends APIMessage {
    @APIParam(required = false, resourceType = AccountVO.class)
    private String accountUuid;

    @APIParam(required = false)
    private String accountName;

    @APIParam(required = false, validEnums = AccountSource.class)
    private String source;

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public static APICreateSessionForZCenterAccountMsg __example__() {
        APICreateSessionForZCenterAccountMsg msg = new APICreateSessionForZCenterAccountMsg();
        msg.setAccountName("test1");
        msg.setSource(AccountSource.Local.name());
        return msg;
    }
}
