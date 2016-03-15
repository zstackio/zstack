package org.zstack.header.console;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.NoJsonSchema;

import java.util.Map;

/**
 * Created by xing5 on 2016/3/15.
 */
public class APIReconnectConsoleProxyAgentEvent extends APIEvent {
    @NoJsonSchema
    private Map<String, Object> inventory;

    public APIReconnectConsoleProxyAgentEvent() {
    }

    public APIReconnectConsoleProxyAgentEvent(String apiId) {
        super(apiId);
    }


    public Map<String, Object> getInventory() {
        return inventory;
    }

    public void setInventory(Map<String, Object> inventory) {
        this.inventory = inventory;
    }
}
