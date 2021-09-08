package org.zstack.core.config;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/global-configurations/{category}/{name}",
        method = HttpMethod.GET,
        responseClass = APIQueryGlobalConfigValidValuesEvent.class
)
public class APIQueryGlobalConfigValidValuesMsg extends APIMessage {
    @APIParam
    private String category;
    @APIParam
    private String name;

    public String getIdentity() {
        return GlobalConfig.produceIdentity(category, name);
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static APIQueryGlobalConfigValidValuesMsg __example__() {
        APIQueryGlobalConfigValidValuesMsg msg = new APIQueryGlobalConfigValidValuesMsg();
        msg.setCategory("kvm");
        msg.setName("vm.cpuMode");
        return msg;
    }
}
