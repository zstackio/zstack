package org.zstack.ldap;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.log.HasSensitiveInfo;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/ldap/servers",
        method = HttpMethod.POST,
        responseClass = APIAddLdapServerEvent.class,
        parameterName = "params"
)
public class APIAddLdapServerMsg extends APIMessage implements APIAuditor, HasSensitiveInfo {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(maxLength = 2048, required = false)
    private String description;

    @APIParam(maxLength = 1024)
    private String url;

    @APIParam(maxLength = 1024)
    private String base;

    @APIParam(maxLength = 1024)
    private String username;

    @APIParam(maxLength = 1024, password = true)
    @NoLogging
    private String password;

    @APIParam(maxLength = 1024, validValues = {"None", "TLS"})
    private String encryption;

    @APIParam(validValues = {"account", "IAM2"})
    private String scope = "account";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEncryption() {
        return encryption;
    }

    public void setEncryption(String encryption) {
        this.encryption = encryption;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public static APIAddLdapServerMsg __example__() {
        APIAddLdapServerMsg msg = new APIAddLdapServerMsg();
        msg.setName("miao");
        msg.setDescription("miao desc");
        msg.setUrl("ldap://localhost:1888");
        msg.setBase("dc=example,dc=com");
        msg.setUsername("");
        msg.setPassword("");
        msg.setEncryption("None");
        msg.setScope(AccountConstant.LOGIN_TYPE);

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIAddLdapServerEvent)rsp).getInventory().getUuid() : "", LdapServerVO.class);
    }
}
