package org.zstack.header.identity;

import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;

public class APICreateAccountMsg extends APICreateMessage {
    @APIParam
    private String name;
    @APIParam
    private String password;
    @APIParam(validValues = {"SystemAdmin", "Normal"}, required = false)
    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}
