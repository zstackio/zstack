package org.zstack.compute.host;

import java.util.*;

public class HostResourceAllocationManager {
    protected String strategy;
    protected String scene;
    protected Map<String, List<String>> allocatedCPUs;
    protected Map<String, Map<String, Object>> numas;
    protected List<Map<String, Object>> vNumas;

    public HostResourceAllocationManager(String strategy, String scene, Map<String, Map<String, Object>> numas, String allocatedCPUs) {
        this.allocatedCPUs = getAllocatedNodeInfo(allocatedCPUs);
        this.numas = numas;
        this.scene = scene;
        this.strategy = strategy;
        this.vNumas = new ArrayList<>();
    }

    public Map<String, List<String>> getAllocatedCPUs() {
        return this.allocatedCPUs;
    }

    public void allocate(int vCPUNum, Long memSize) {
        if (vCPUNum == 0) {return ;}

        HostResourceAllocationStrategy allocator = new HostResourceAllocationStrategy(this.numas, this.allocatedCPUs);
        boolean cycle = false;
        if (this.scene.equals("normal")) {
            cycle = true;
            memSize = 0L;
        } else {
            if (!allocator.isCPUEnough(vCPUNum) || (!allocator.isMemSizeEnough(memSize))) {
                return ;
            }
        }
        if (this.strategy.equals("continuous")) {
            allocator = new ContinuousDistribution(this.numas, this.allocatedCPUs);
        }
        this.vNumas = allocator.allocate(vCPUNum, memSize, cycle);
        this.allocatedCPUs = allocator.getAllocatedCPUs();
    }

    public List<Map<String, String>> getCPUPins() {
        List<Map<String, String>> pins = new ArrayList<>();

        Integer vCPUID = (Integer) 0;
        for(Map<String, Object> node: this.vNumas) {
            String nodeID = (String) node.get("nodeID");
            List<String> pCPUIDs = (List<String>) node.get("CPUs");
            for (String pCPUID:pCPUIDs) {
                Map<String, String> pin = new HashMap<>();
                pin.put("vCPU", vCPUID.toString());
                pin.put("pCPU", pCPUID);
                vCPUID += 1;
                pins.add(pin);
            }
        }
        return pins;
    }

    public static Map<String, List<String>> getAllocatedNodeInfo(String allocatedCPUs) {
        String[] nodeString = allocatedCPUs.split(";");
        Map<String, List<String>> allocatedNodeInfo = new HashMap<>();
        for (String node: nodeString) {
            if (node.isEmpty()) {continue;}
            String[] temp = node.split(":");
            String[] CPUList = temp[1].split(",");
            allocatedNodeInfo.put(temp[0], Arrays.asList(CPUList));
        }
        return allocatedNodeInfo;
    }

    public String getAllocatedNodeInfo() {
        StringBuffer allocatedNodeInfo = new StringBuffer();
        for (String nodeID: this.allocatedCPUs.keySet()) {
            String CPUString = String.join(",", this.allocatedCPUs.get(nodeID));
            allocatedNodeInfo.append(String.format("%s:%s", nodeID, CPUString));
            allocatedNodeInfo.append(";");
        }
        if (allocatedNodeInfo == null) {
            return "";
        } else {
            return allocatedNodeInfo.toString();
        }
    }
}
