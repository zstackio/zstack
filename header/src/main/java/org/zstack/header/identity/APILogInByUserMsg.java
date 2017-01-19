package org.zstack.header.identity;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@SuppressCredentialCheck
@RestRequest(
        path = "/accounts/users/login",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APILogInReply.class
)
public class APILogInByUserMsg extends APISessionMessage {
    @APIParam(required = false)
    private String accountUuid;
    @APIParam(required = false)
    private String accountName;
    @APIParam
    private String userName;
    @APIParam
    private String password;

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
 
    public static APILogInByUserMsg __example__() {
        APILogInByUserMsg msg = new APILogInByUserMsg();

        msg.setAccountName("test");
        msg.setUserName("user");
        msg.setPassword("password");

        return msg;
    }

}
