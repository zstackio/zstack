package org.zstack.core.config;

import org.zstack.header.message.APIListMessage;

import java.util.List;

import static java.util.Arrays.asList;

public class APIListGlobalConfigMsg extends APIListMessage {
	private List<Long> ids;
	
    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }



    public static APIListGlobalConfigMsg __example__() {
        APIListGlobalConfigMsg msg = new APIListGlobalConfigMsg();
        return msg;
    }
    
}