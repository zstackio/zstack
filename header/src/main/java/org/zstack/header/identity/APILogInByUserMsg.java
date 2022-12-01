package org.zstack.header.identity;

import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APIReply;
import org.zstack.header.other.APILoginAuditor;
import org.zstack.header.rest.RestRequest;

import java.util.Map;

@SuppressCredentialCheck
@RestRequest(
        path = "/accounts/users/login",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APILogInReply.class
)
public class APILogInByUserMsg extends APISessionMessage implements APILoginAuditor {
    @APIParam(required = false)
    private String accountUuid;
    @APIParam(required = false)
    private String accountName;
    @APIParam
    private String userName;
    @APIParam(password = true)
    @NoLogging
    private String password;
    @APIParam(required = false)
    private Map<String, String> clientInfo;

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

    public void setPassword(String password) {
        this.password = password;
    }

    public Map<String, String> getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(Map<String, String> clientInfo) {
        this.clientInfo = clientInfo;
    }

    @Override
    public String getOperator() {
        return userName;
    }

    public static APILogInByUserMsg __example__() {
        APILogInByUserMsg msg = new APILogInByUserMsg();

        msg.setAccountName("test");
        msg.setUserName("user");
        msg.setPassword("password");

        return msg;
    }

    @Override
    public LoginResult loginAudit(APIMessage msg, APIReply reply) {
        String clientIp = "";
        String clientBrowser = "";
        APILogInByUserMsg amsg = (APILogInByUserMsg) msg;
        Map<String, String> clientInfo = amsg.getClientInfo();
        if (clientInfo != null && !clientInfo.isEmpty()) {
            clientIp = StringUtils.isNotEmpty(clientInfo.get("clientIp")) ? clientInfo.get("clientIp") : "";
            clientBrowser = StringUtils.isNotEmpty(clientInfo.get("clientBrowser")) ? clientInfo.get("clientBrowser") : "";
        }
        String resourceUuid = reply.isSuccess() ? ((APILogInReply) reply).getInventory().getUuid() : "";
        return new LoginResult(clientIp, clientBrowser, resourceUuid, SessionVO.class);
    }

    @Override
    public String getUsername() {
        return userName;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getLoginType() {
        return AccountConstant.LOGIN_TYPE;
    }
}
