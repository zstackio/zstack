package org.zstack.header.host;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;


import java.util.*;

@RestResponse
public class APIQueryHostNUMATopologyReply extends APIQueryReply {
    private String name;
    private String uuid;
    private List<Map<String,Object>> topology;



    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return this.uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setTopology(List<Map<String,Object>> topology) {
        this.topology = topology;
    }

    public List<Map<String,Object>> getTopology() {
        return this.topology;
    }

    public static APIQueryHostNUMATopologyReply __example__() {
        APIQueryHostNUMATopologyReply reply = new APIQueryHostNUMATopologyReply();

        List<Map<String,Object>> topology = new ArrayList<Map<String,Object>> ();
        Map<String, Object> node = new HashMap<String, Object>();
        node.put("nodeID", 0);
        node.put("memSize", 12345);
        node.put("availableMemSize", 10000);

        List<String> vmsUuid = new ArrayList<String>();
        vmsUuid.add("f7bae73b9874344b8766dfcdda48ad6e");
        node.put("VMsUuid", vmsUuid);

        List<String> cpusID = new ArrayList<String>();
        cpusID.add("0");
        node.put("CPUsID", cpusID);

        List<String> usedCPUs = new ArrayList<String>();
        usedCPUs.add("0");
        node.put("usedCPUs", usedCPUs);


        topology.add(node);

        reply.setTopology(topology);
        reply.setName("example");
        reply.setUuid("f7bae73b9874344b8766dfcdda48ad6e");
        return reply;
    }
}
