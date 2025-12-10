package org.zstack.header.securitymachine.api;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by xing5 on 2016/4/8.
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetPreAuthenticationConfigReply extends APIReply {
    private Map<String, String> configs;

    public Map<String, String> getConfigs() {
        return configs;
    }

    public void setConfigs(Map<String, String> configs) {
        this.configs = configs;
    }

    public static APIGetPreAuthenticationConfigReply __example__() {
        APIGetPreAuthenticationConfigReply reply = new APIGetPreAuthenticationConfigReply();
        Map<String, String> inventories = new HashMap<>();
        inventories.put(uuid(), uuid());
        reply.setConfigs(inventories);
        return reply;
    }

}
