package org.zstack.core.config;

import org.zstack.header.message.APIListMessage;

import java.util.List;

public class APIListGlobalConfigMsg extends APIListMessage {
	private List<Long> ids;
	
    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }
}
