package org.zstack.ldap;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

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
}
