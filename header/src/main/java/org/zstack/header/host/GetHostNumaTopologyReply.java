package org.zstack.header.host;

import org.zstack.header.message.MessageReply;

import java.util.Map;

public class GetHostNumaTopologyReply extends MessageReply {
    private Map<String, Map<String, Object>> numa;

    public void setNuma(Map<String, Map<String, Object>> numa) {
        this.numa = numa;
    }

    public Map<String, Map<String, Object>> getNuma() {
        return numa;
    }
}
