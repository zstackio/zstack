package org.zstack.header.host;


import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RestResponse;


import java.util.*;

@RestResponse(fieldsTo = {"name", "uuid", "topology"})
public class APIQueryHostNUMATopologyReply extends MessageReply {
    private String name;
    private String uuid;
    private Map<String, Map<String, Object>> topology;

    public void setName(String name) {
        this.name = name;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setTopology(Map<String, Map<String, Object>> topology) {
        this.topology = topology;
    }

    public Map<String, Map<String, Object>> getTopology() {
        return topology;
    }

    public String getName() {
        return name;
    }

    public String getUuid() {
        return uuid;
    }
}
