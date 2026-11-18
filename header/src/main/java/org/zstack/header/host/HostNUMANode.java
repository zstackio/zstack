package org.zstack.header.host;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

public class HostNUMANode {
    public List<String> distance;
    public List<String> cpus;
    public List<String> onlineCpus;
    public List<List<String>> coreGroups;
    public Long free;
    public Long size;
    public String nodeID;
    public List<String> VMsUuid;

    public String getNodeID() {
        return nodeID;
    }

    public void setNodeID(String nodeID) {
        this.nodeID = nodeID;
    }

    public List<String> getDistance() {
        return distance;
    }

    public void setDistance(List<String> distance) {
        this.distance = distance;
    }

    public List<String> getCpus() {
        return cpus;
    }

    public void setCpus(List<String> cpus) {
        this.cpus = cpus;
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

    public Long getFree() {
        return free;
    }

    public void setFree(Long free) {
        this.free = free;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public List<String> getVMsUuid() {
        return VMsUuid;
    }

    public void setVMsUuid(List<String> VMsUuid) {
        this.VMsUuid = VMsUuid;
    }

    public static HostNUMANode __example__() {
        HostNUMANode node = new HostNUMANode();
        node.setNodeID("1");
        node.setDistance(list("0", "1"));
        node.setCpus(list("0", "1"));
        node.setFree(1L);
        node.setSize(2L);
        node.setVMsUuid(list("vm-1", "vm-2"));
        return node;
    }
}
