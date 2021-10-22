package org.zstack.compute.host;


import java.util.*;

public class ContinuousDistribution extends HostResourceAllocationStrategy {
    public ContinuousDistribution(Map<String, Map<String, Object>> numas, Map<String, List<String>> allocatedCPUs) {
        super(numas, allocatedCPUs);
    }

    @Override
    public List<Map<String, Object>> allocate(int vCPUNum, Long memSize, boolean cycle) {

        int index = this.nodesNum - 1;
        while(true) {
            // 判断是否遍历结束,如果遍历结束且需要再次重新分配则将下表重新设置为最大的nodeID,同时清除已被分配CPU信息
            if (index < 0) {
                if (cycle) {
                    index= this.nodesNum - 1;
                    this.allocatedCPUs.clear();
                } else {
                    break;
                }
            }

            String nodeID = String.valueOf(index);
            List<String> nodeCPUs = new ArrayList<>(this.nodesCPUInfo.get(index));


            if (this.allocatedCPUs.containsKey(nodeID)) {
                nodeCPUs.removeAll(this.allocatedCPUs.get(nodeID));
                if (nodeCPUs.size() == 0) {
                    index -= 1;
                    continue;
                }
            }

            Map<String, Object> node = new HashMap<>();
            node.put("nodeID", nodeID);

            Long nodeMemSize = this.nodesMemInfo.get(index);
            List<String> distance = this.nodesDistance.get(index);

            if (nodeCPUs.size() <= 0) {
                index -= 1;
                continue;
            }

            if (vCPUNum <= nodeCPUs.size()) {
                float percent = (float) vCPUNum / (float) nodeCPUs.size();
                long allocationMemSize = (long) (nodeMemSize*percent);

                node.put("memSize", allocationMemSize);
                memSize -= allocationMemSize;
                List<String> CPUs = new ArrayList<>(nodeCPUs.subList(0, vCPUNum));
                node.put("CPUs", CPUs);

                node.put("distance", distance);

                this.addNodeIntovNuma(nodeID, node);


                this.updateAllocatedCPUs(nodeID, CPUs);

                vCPUNum = 0;
                index -= 1;
                break;
            }

            node.put("memSize", nodeMemSize);
            memSize -= nodeMemSize;
            node.put("CPUs", nodeCPUs);
            vCPUNum -= nodeCPUs.size();
            node.put("distance", distance);

            this.updateAllocatedCPUs(nodeID, nodeCPUs);

            this.addNodeIntovNuma(nodeID, node);
            index -= 1;

        }
        return new ArrayList<>(this.allocatedNodes.values());
    }

}
