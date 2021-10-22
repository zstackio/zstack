package org.zstack.compute.host;

import java.util.*;

public class HostResourceAllocationManager {
    protected String strategy;
    protected String scene;
    protected Map<String, List<String>> allocatedCPUs;
    protected Map<String, Map<String, Object>> numas;
    protected List<Map<String, Object>> vNumas;
    private HostResourceAllocationStrategy allocator;

    public HostResourceAllocationManager(String strategy, String scene, Map<String, Map<String, Object>> numas, String allocatedCPUs) {
        this.allocatedCPUs = getAllocatedNodeInfo(allocatedCPUs);
        this.numas = numas;
        this.scene = scene;
        this.strategy = strategy;
        this.vNumas = new ArrayList<>();
        this.allocator = new HostResourceAllocationStrategy(this.numas, this.allocatedCPUs);
    }

    public HostResourceAllocationManager() {}

    public void setAllocatedCPUs(Map<String, List<String>> allocatedCPUs) {
        this.allocatedCPUs = allocatedCPUs;
    }

    public void setAllocatedCPUs(String allocatedCPUs) {
        this.allocatedCPUs = getAllocatedNodeInfo(allocatedCPUs);
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public void setNumas(Map<String, Map<String, Object>> numas) {
        this.numas = numas;
    }

    public void setvNumas(List<Map<String, Object>> vNumas) {
        this.vNumas = vNumas;
    }

    public HostResourceAllocationStrategy getAllocator() {
        return allocator;
    }

    public void setAllocator(HostResourceAllocationStrategy allocator) {
        this.allocator = allocator;
    }

    public String getStrategy() {
        return strategy;
    }

    public String getScene() {
        return scene;
    }

    public List<Map<String, Object>> getvNumas() {
        return vNumas;
    }

    public Map<String, Map<String, Object>> getNumas() {
        return numas;
    }

    public Map<String, List<String>> getAllocatedCPUs() {
        return this.allocatedCPUs;
    }

    public void allocate(int vCPUNum, Long memSize) {
        if (vCPUNum == 0) {return ;}

        boolean cycle = false;
        if (this.scene.equals("normal")) {
            cycle = true;
            memSize = 0L;
        } else {
            if (!this.allocator.isCPUEnough(vCPUNum) || (!this.allocator.isMemSizeEnough(memSize))) {
                return ;
            }
        }
        if (this.strategy.equals("continuous")) {
            this.allocator = new ContinuousDistribution(this.numas, this.allocatedCPUs);
        }
        this.vNumas = this.allocator.allocate(vCPUNum, memSize, cycle);
        this.allocatedCPUs = this.allocator.getAllocatedCPUs();
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
        if (allocatedNodeInfo.length() == 0) {
            return "";
        } else {
            return allocatedNodeInfo.toString();
        }
    }

    public Map<String, Object> getvNUMAConfiguration() {
        int nodeID = 0;
        int vCPUID = 0;
        Map<String, String> vCPUPins = new HashMap<>();
        Map<String, Object> nodes = new HashMap<>();
        List<Integer> pNodes = new ArrayList<>();

        for (Map<String, Object> node: this.vNumas) {
            String pNodeID = (String) node.get("nodeID");
            String startvCPUID = String.valueOf(vCPUID);

            List<String> CPUList = (List<String>) node.get("CPUs");
            for (String pCPUID: CPUList) {
                vCPUPins.put(String.valueOf(vCPUID), pCPUID);
                vCPUID += 1;
            }

            Map<String, Object> vNode = new HashMap<>();
            pNodes.add(Integer.parseInt(pNodeID));
            vNode.put("pNodeID", pNodeID);
            vNode.put("CPUs", String.format("%s-%d", startvCPUID, vCPUID-1));
            vNode.put("memSize", node.get("memSize"));
            nodes.put(String.valueOf(nodeID), vNode);

            nodeID += 1;
        }


        for (Integer pNodeID: pNodes) {
            List<String> pNodeDistance = new ArrayList<>();
            for (Integer anotherPNode: pNodes) {
                 pNodeDistance.add(this.allocator.getNodesDistance().get(pNodeID).get(anotherPNode));
            }
            Map<String, Object> vNode = (Map<String, Object>) nodes.get(String.valueOf(pNodes.indexOf(pNodeID)));
            vNode.put("distance", pNodeDistance);
        }
        return nodes;
    }
}
