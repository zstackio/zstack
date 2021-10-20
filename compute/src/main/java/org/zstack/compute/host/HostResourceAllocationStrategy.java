package org.zstack.compute.host;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HostResourceAllocationStrategy {
    protected List<List<String>> nodesCPUInfo;
    protected List<Long> nodesMemInfo;
    protected List<List<String>> nodesDistance = new ArrayList<>();
    protected int CPUNumPerNode;
    protected int nodesNum;
    protected int availableCPUNum;
    protected Long availableMemSize;
    protected Map<String, List<String>> allocatedCPUs;

    public HostResourceAllocationStrategy(Map<String, Map<String, Object>> numas,
                                          Map<String, List<String>> allocatedCPUs) {
        this.nodesMemInfo = new ArrayList<>();
        this.nodesCPUInfo = new ArrayList<>();
        this.CPUNumPerNode = 0;
        this.nodesNum = numas.size();
        this.availableCPUNum = 0;
        this.availableMemSize = 0L;
        this.allocatedCPUs = allocatedCPUs;

        if (!numas.isEmpty()) {
            this.CPUNumPerNode = ((List) ((Map<String, Object>) numas.get("0")).get("cpus")).size();
        }

        Integer nodeID= (Integer) 0;
        while (numas.containsKey(nodeID.toString())) {
            Map<String, Object> node = numas.get(nodeID.toString());
            List<String> nodeCPUs = (List<String>) node.get("cpus");

            if (nodeCPUs.isEmpty()) {continue;}
            this.nodesCPUInfo.add(nodeCPUs);
            this.availableCPUNum += nodeCPUs.size();

            Float availableMemSize = Float.parseFloat(String.valueOf(node.get("free")));
            Long nodeMemSize = availableMemSize.longValue();
            this.nodesMemInfo.add(nodeMemSize);
            this.availableMemSize += nodeMemSize;

            this.nodesDistance.add((List<String>) node.get("distance"));

            nodeID += 1;
        }
    }

    public boolean isCPUEnough(int CPUNum) {
        return CPUNum <= this.availableCPUNum;
    }

    public boolean isMemSizeEnough(Long memSize) {
        return memSize <= this.availableMemSize;
    }

    public Map<String, List<String>> getAllocatedCPUs() {
        return allocatedCPUs;
    }

    public List<Map<String, Object>> allocate(int vCPUNum, Long memSize, boolean cycle) {
        return new ArrayList<>();
    };
}
