package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@SuppressCredentialCheck
@RestRequest(
        path = "/accounts/login",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APILogInReply.class
)
public class APILogInByAccountMsg extends APISessionMessage {
    @APIParam
    private String accountName;
    @APIParam
    private String password;

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
