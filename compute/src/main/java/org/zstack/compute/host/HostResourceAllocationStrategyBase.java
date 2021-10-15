package org.zstack.compute.host;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HostResourceAllocationStrategyBase {
    protected List<List<Integer>> nodesCPUInfo = new ArrayList<>();
    protected List<Long> nodesMemInfo = new ArrayList<>();
    protected List<List<String>> nodesDistance = new ArrayList<>();
    protected int CPUNumPerNode;
    protected int nodesNum;
    protected int availableCPUNum = 0;
    protected Long availableMemSize = 0L;

    public HostResourceAllocationStrategyBase(Map<String, Map<String, Object>> numa) {
        this.CPUNumPerNode = ((List) ((Map<String, Object>) numa.get("0")).get("cpus")).size();
        this.nodesNum = numa.size();
        Integer nodeID= (Integer) 0;
        while (numa.containsKey(nodeID.toString())) {
            Map<String, Object> node = numa.get(nodeID.toString());
            List<Integer> nodeCPUs = (List<Integer>) node.get("cpus");
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

    public List<Map<String, String>> allocate(int vCPU, Long memSize) {
        List<Map<String, String>> pins = new ArrayList<>();
        return pins;
    }
}
