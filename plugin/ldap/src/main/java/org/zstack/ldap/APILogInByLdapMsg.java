package org.zstack.ldap;

import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpMethod;
import org.zstack.header.identity.APILogInReply;
import org.zstack.header.identity.APISessionMessage;
import org.zstack.header.identity.SessionVO;
import org.zstack.header.identity.SuppressCredentialCheck;
import org.zstack.header.identity.login.APICaptchaMessage;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APIReply;
import org.zstack.header.other.APILoginAuditor;
import org.zstack.header.rest.RestRequest;

import java.util.Map;

@SuppressCredentialCheck
@RestRequest(
        path = "/ldap/login",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APILogInByLdapReply.class
)
public class APILogInByLdapMsg extends APISessionMessage implements APILoginAuditor, APICaptchaMessage {
    @APIParam
    private String uid;
    @APIParam(password = true)
    @NoLogging
    private String password;
    @APIParam(required = false)
    private String verifyCode;
    @APIParam(required = false)
    private String captchaUuid;
    @APIParam(required = false)
    private Map<String, String> clientInfo;

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getVerifyCode() {
        return verifyCode;
    }

    public void setVerifyCode(String verifyCode) {
        this.verifyCode = verifyCode;
    }

    public String getCaptchaUuid() {
        return captchaUuid;
    }

    public void setCaptchaUuid(String captchaUuid) {
        this.captchaUuid = captchaUuid;
    }

    public Map<String, String> getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(Map<String, String> clientInfo) {
        this.clientInfo = clientInfo;
    }

    @Override
    public String getOperator() {
        return uid;
    }

    public static APILogInByLdapMsg __example__() {
        APILogInByLdapMsg msg = new APILogInByLdapMsg();
        msg.setUid("ou=Employee,uid=test");
        msg.setPassword("password");
        msg.setCaptchaUuid("3bb4e611b10e32cfaaa7fb4b1e3515ac");
        msg.setVerifyCode("test");
        return msg;
    }

    @Override
    public LoginResult loginAudit(APIMessage msg, APIReply reply) {
        String clientIp = "";
        String clientBrowser = "";
        APILogInByLdapMsg amsg = (APILogInByLdapMsg) msg;
        Map<String, String> clientInfo = amsg.getClientInfo();
        if (clientInfo != null && !clientInfo.isEmpty()) {
            clientIp = StringUtils.isNotEmpty(clientInfo.get("clientIp")) ? clientInfo.get("clientIp") : "";
            clientBrowser = StringUtils.isNotEmpty(clientInfo.get("clientBrowser")) ? clientInfo.get("clientBrowser") : "";
        }
        String resourceUuid = reply.isSuccess() ? ((APILogInByLdapReply) reply).getInventory().getUuid() : "";
        return new LoginResult(clientIp, clientBrowser, resourceUuid, SessionVO.class);
    }

    @Override
    public String getUsername() {
        return uid;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getLoginType() {
        return LdapConstant.LOGIN_TYPE;
    }
}
