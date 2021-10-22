package org.zstack.compute.host;

import java.util.*;

public class HostResourceAllocationStrategy {
    protected List<List<String>> nodesCPUInfo;
    protected List<Long> nodesMemInfo;
    protected List<List<String>> nodesDistance = new ArrayList<>();
    protected int CPUNumPerNode;
    protected int nodesNum;
    protected int availableCPUNum;
    protected Long availableMemSize;
    protected Map<String, List<String>> allocatedCPUs;
    protected Map<String, Map<String, Object>> allocatedNodes;

    public HostResourceAllocationStrategy(Map<String, Map<String, Object>> numas,
                                          Map<String, List<String>> allocatedCPUs) {
        this.nodesMemInfo = new ArrayList<>();
        this.nodesCPUInfo = new ArrayList<>();
        this.CPUNumPerNode = 0;
        this.nodesNum = numas.size();
        this.availableCPUNum = 0;
        this.availableMemSize = 0L;
        this.allocatedCPUs = allocatedCPUs;
        this.allocatedNodes = new HashMap<>();

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

    //负责分配pNode给vNUMA
    public List<Map<String, Object>> allocate(int vCPUNum, Long memSize, boolean cycle) {
        return new ArrayList<>();
    };

    //记录分配的node到NUMA中如果新增加的node与已分配的node重复则合并node
    //只有scene为normal时因为超分导致同一个node被多次分配,因而出现重复的被分配的node
    public void addNodeIntovNuma(String nodeID, Map<String, Object> node) {
        if (this.allocatedNodes.containsKey(nodeID)) {
            Map<String, Object> oldNode = this.allocatedNodes.get(nodeID);
            List<String> CPUs = (List<String>) oldNode.get("CPUs");
            CPUs.addAll((List<String>) node.get("CPUs"));
            Long oldMemSize = (Long) oldNode.get("memSize");
            Long newMwmSize = (Long) node.get("memSize");
            oldNode.put("memSize", oldMemSize < newMwmSize? newMwmSize:oldMemSize);
            oldNode.put("CPUs", CPUs);
            this.allocatedNodes.put(nodeID, oldNode);
        } else {
            this.allocatedNodes.put(nodeID, node);
        }
    }

    //更新已分配对应NUMA node的CPU列表
    public void updateAllocatedCPUs(String nodeID, List<String> nodeCPUs) {
        this.allocatedCPUs.compute(nodeID, (k, v) -> {
            if ((v == null) || (v.isEmpty())) {
                v = nodeCPUs;
            } else {
                Set<String> sv = new HashSet<>(v);
                sv.addAll(nodeCPUs);
                v = new ArrayList<>(sv);
            }
            return v;
        });
    }

    //计算CPU数量是否能够满足云主机vCPU数量
    public boolean isCPUEnough(int CPUNum) {
        return CPUNum <= this.availableCPUNum;
    }

    //计算总可用内存大小是否满足云主机内存需求
    public boolean isMemSizeEnough(Long memSize) {
        return memSize <= this.availableMemSize;
    }

    public Map<String, List<String>> getAllocatedCPUs() {
        return allocatedCPUs;
    }

    public void setAllocatedCPUs(Map<String, List<String>> allocatedCPUs) {
        this.allocatedCPUs = allocatedCPUs;
    }

    public void setAvailableCPUNum(int availableCPUNum) {
        this.availableCPUNum = availableCPUNum;
    }

    public void setAvailableMemSize(Long availableMemSize) {
        this.availableMemSize = availableMemSize;
    }

    public void setCPUNumPerNode(int CPUNumPerNode) {
        this.CPUNumPerNode = CPUNumPerNode;
    }

    public void setNodesCPUInfo(List<List<String>> nodesCPUInfo) {
        this.nodesCPUInfo = nodesCPUInfo;
    }

    public void setNodesDistance(List<List<String>> nodesDistance) {
        this.nodesDistance = nodesDistance;
    }

    public void setNodesMemInfo(List<Long> nodesMemInfo) {
        this.nodesMemInfo = nodesMemInfo;
    }

    public void setNodesNum(int nodesNum) {
        this.nodesNum = nodesNum;
    }

    public int getAvailableCPUNum() {
        return availableCPUNum;
    }

    public int getCPUNumPerNode() {
        return CPUNumPerNode;
    }

    public int getNodesNum() {
        return nodesNum;
    }

    public List<List<String>> getNodesCPUInfo() {
        return nodesCPUInfo;
    }

    public List<List<String>> getNodesDistance() {
        return nodesDistance;
    }

    public List<Long> getNodesMemInfo() {
        return nodesMemInfo;
    }

    public Long getAvailableMemSize() {
        return availableMemSize;
    }
}
