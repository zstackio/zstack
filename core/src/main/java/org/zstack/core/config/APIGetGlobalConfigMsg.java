package org.zstack.core.config;

import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;

public class APIGetGlobalConfigMsg extends APISyncCallMessage {
    @APIParam
    private String category;
    @APIParam
    private String name;
    
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

    public String getIdentity() {
        return GlobalConfig.produceIdentity(category, name);
    }



    public static APIGetGlobalConfigMsg __example__() {
        APIGetGlobalConfigMsg msg = new APIGetGlobalConfigMsg();
        return msg;
    }
    
}