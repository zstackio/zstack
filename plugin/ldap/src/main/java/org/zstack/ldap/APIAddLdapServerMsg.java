package org.zstack.ldap;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/ldap/servers",
        method = HttpMethod.POST,
        responseClass = APIAddLdapServerEvent.class,
        parameterName = "params"
)
public class APIAddLdapServerMsg extends APIMessage {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(maxLength = 2048)
    private String description;

    @APIParam(maxLength = 1024)
    private String url;

    @APIParam(maxLength = 1024)
    private String base;

    @APIParam(maxLength = 1024)
    private String username;

    @APIParam(maxLength = 1024)
    private String password;

    @APIParam(maxLength = 1024, validValues = {"None", "TLS"})
    private String encryption;

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
 
    public static APIAddLdapServerMsg __example__() {
        APIAddLdapServerMsg msg = new APIAddLdapServerMsg();
        msg.setName("miao");
        msg.setDescription("miao desc");
        msg.setUrl("ldap://localhost:1888");
        msg.setBase("dc=example,dc=com");
        msg.setUsername("");
        msg.setPassword("");
        msg.setEncryption("None");

        return msg;
    }

}
