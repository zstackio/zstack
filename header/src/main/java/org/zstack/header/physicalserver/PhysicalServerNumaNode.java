package org.zstack.header.physicalserver;

import java.util.ArrayList;
import java.util.List;

public class PhysicalServerNumaNode {
    private String nodeId;
    private List<String> onlineCpus = new ArrayList<>();
    private List<List<String>> coreGroups = new ArrayList<>();

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public List<String> getOnlineCpus() {
        return onlineCpus;
    }

    public void setOnlineCpus(List<String> onlineCpus) {
        this.onlineCpus = onlineCpus;
    }

    public List<List<String>> getCoreGroups() {
        return coreGroups;
    }

    public void setCoreGroups(List<List<String>> coreGroups) {
        this.coreGroups = coreGroups;
    }
}
