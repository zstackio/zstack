package org.zstack.compute.host;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContinuousDistribution extends HostResourceAllocationStrategyBase {

    public ContinuousDistribution(Map<String, Map<String, Object>> numa) {
        super(numa);
    }

    public  List<Map<String, String>> allocate(int vCPUNum, Long memSize) {
        List<Map<String, String>> pins = new ArrayList<>();

        if (!this.isCPUEnough(vCPUNum) | !this.isMemSizeEnough(memSize)) {
            return pins;
        }

        int nodes_num = (int) Math.ceil((float) vCPUNum / (float) this.CPUNumPerNode);
        if (nodes_num > this.nodesNum) {
            return pins;
        }

        Map<Integer, Object> numa = new HashMap<>();
        boolean needBreak = false;

        for ( int index = this.nodesNum -1; index >= 0; index--) {
            Map<String, Object> node = new HashMap<>();
            List<Integer> nodeCPUs = this.nodesCPUInfo.get(index);
            Long nodeMemSize = this.nodesMemInfo.get(index);
            List<String> distance = this.nodesDistance.get(index);

            if (!(nodeCPUs.size() > 0)) {
                continue;
            }

            if (vCPUNum <= nodeCPUs.size()) {
                float percent = (float) vCPUNum / (float) nodeCPUs.size();
                long allocationMemSize = (long) (nodeMemSize*percent);

                node.put("memSize", allocationMemSize);
                memSize -= allocationMemSize;
                node.put("CPUs", nodeCPUs.subList(0, vCPUNum));
                vCPUNum = 0;
                node.put("distance", distance);

                numa.put(index, node);
                needBreak = true;
            }

            if (needBreak) {
                break;
            }

            node.put("memSize", nodeMemSize);
            memSize -= nodeMemSize;
            node.put("CPUs", nodeCPUs);
            vCPUNum -= nodeCPUs.size();
            node.put("distance", distance);

            numa.put(index, node);

        }

        Integer vCPUID = (Integer) 0;
        for(Object node: numa.values()) {
            Map<String, Object> value = (Map<String, Object>) node;
            List<String> pCPUIDs = (List<String>) value.get("CPUs");
            for (String pCPUID:pCPUIDs) {
                Map<String, String> pin = new HashMap<>();
                pin.put("vCPU", vCPUID.toString());
                pin.put("pCPU", pCPUID);
                pins.add(pin);
            }

        }

        return pins;
    }
}
