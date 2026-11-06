package org.zstack.sdnController.header;

import org.zstack.core.db.Q;
import org.zstack.header.host.HostInventory;

import javax.persistence.Tuple;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class HardwareVxlanHelper {
    public static class VxlanHostMappingStruct {
        Integer vlanId;
        String  physicalInterface;

        public VxlanHostMappingStruct() {
        }

        public Integer getVlanId() {
            return vlanId;
        }

        public void setVlanId(Integer vlanId) {
            this.vlanId = vlanId;
        }

        public String getPhysicalInterface() {
            return physicalInterface;
        }

        public void setPhysicalInterface(String physicalInterface) {
            this.physicalInterface = physicalInterface;
        }
    }

    public static VxlanHostMappingStruct getHardwareVxlanMappingVxlanId(HardwareL2VxlanNetworkInventory vxlan, HostInventory host) {
        String phyNic = Q.New(HardwareL2VxlanNetworkPoolVO.class)
                .eq(HardwareL2VxlanNetworkPoolVO_.uuid, vxlan.getPoolUuid())
                .select(HardwareL2VxlanNetworkPoolVO_.physicalInterface).findValue();

        VxlanHostMappingStruct struct = new VxlanHostMappingStruct();
        VxlanHostMappingVO hvo = Q.New(VxlanHostMappingVO.class)
                .eq(VxlanHostMappingVO_.vxlanUuid, vxlan.getUuid())
                .eq(VxlanHostMappingVO_.hostUuid, host.getUuid())
                .find();
        if (hvo != null) {
            struct.setVlanId(hvo.getVlanId());
            if(hvo.getPhysicalInterface() != null) {
                struct.setPhysicalInterface(hvo.getPhysicalInterface());
            } else {
                struct.setPhysicalInterface(phyNic);
            }
            return struct;
        }

        VxlanClusterMappingVO cvo = Q.New(VxlanClusterMappingVO.class)
                .eq(VxlanClusterMappingVO_.clusterUuid, host.getClusterUuid())
                .eq(VxlanClusterMappingVO_.vxlanUuid, vxlan.getUuid())
                .find();
        if (cvo != null) {
            struct.setVlanId(cvo.getVlanId());
            if(cvo.getPhysicalInterface() != null) {
                struct.setPhysicalInterface(cvo.getPhysicalInterface());
            } else {
                struct.setPhysicalInterface(phyNic);
            }
            return struct;
        }

        if (vxlan.getVlan() != 0) {
            struct.setVlanId(vxlan.getVlan());
        } else {
            struct.setVlanId(vxlan.getVirtualNetworkId());
        }
        struct.setPhysicalInterface(phyNic);
        return struct;
    }

    public static Map<String, VxlanHostMappingStruct> getHardwareVxlanMappingVxlanIds(List<HardwareL2VxlanNetworkInventory> vxlans, HostInventory host) {
        if (vxlans == null || vxlans.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<String> poolUuids = vxlans.stream().map(HardwareL2VxlanNetworkInventory::getPoolUuid).collect(Collectors.toSet());
        List<String> vxlanUuids = vxlans.stream().map(HardwareL2VxlanNetworkInventory::getUuid).collect(Collectors.toList());

        Map<String, String> poolPhyNicMap = new HashMap<>();
        List<Tuple> poolTuples = Q.New(HardwareL2VxlanNetworkPoolVO.class)
                .in(HardwareL2VxlanNetworkPoolVO_.uuid, poolUuids)
                .select(HardwareL2VxlanNetworkPoolVO_.uuid, HardwareL2VxlanNetworkPoolVO_.physicalInterface)
                .listTuple();
        for (Tuple t : poolTuples) {
            poolPhyNicMap.put(t.get(0, String.class), t.get(1, String.class));
        }

        List<VxlanHostMappingVO> hostMappings = Q.New(VxlanHostMappingVO.class)
                .in(VxlanHostMappingVO_.vxlanUuid, vxlanUuids)
                .eq(VxlanHostMappingVO_.hostUuid, host.getUuid())
                .list();
        Map<String, VxlanHostMappingVO> hostMappingMap = hostMappings.stream()
                .collect(Collectors.toMap(VxlanHostMappingVO::getVxlanUuid, Function.identity(), (a, b) -> a));

        List<VxlanClusterMappingVO> clusterMappings = Q.New(VxlanClusterMappingVO.class)
                .eq(VxlanClusterMappingVO_.clusterUuid, host.getClusterUuid())
                .in(VxlanClusterMappingVO_.vxlanUuid, vxlanUuids)
                .list();
        Map<String, VxlanClusterMappingVO> clusterMappingMap = clusterMappings.stream()
                .collect(Collectors.toMap(VxlanClusterMappingVO::getVxlanUuid, Function.identity(), (a, b) -> a));

        Map<String, VxlanHostMappingStruct> result = new HashMap<>(vxlans.size());
        for (HardwareL2VxlanNetworkInventory vxlan : vxlans) {
            String phyNic = poolPhyNicMap.get(vxlan.getPoolUuid());
            VxlanHostMappingStruct struct = new VxlanHostMappingStruct();

            VxlanHostMappingVO hvo = hostMappingMap.get(vxlan.getUuid());
            if (hvo != null) {
                struct.setVlanId(hvo.getVlanId());
                struct.setPhysicalInterface(hvo.getPhysicalInterface() != null ? hvo.getPhysicalInterface() : phyNic);
                result.put(vxlan.getUuid(), struct);
                continue;
            }

            VxlanClusterMappingVO cvo = clusterMappingMap.get(vxlan.getUuid());
            if (cvo != null) {
                struct.setVlanId(cvo.getVlanId());
                struct.setPhysicalInterface(cvo.getPhysicalInterface() != null ? cvo.getPhysicalInterface() : phyNic);
                result.put(vxlan.getUuid(), struct);
                continue;
            }

            struct.setVlanId(vxlan.getVlan() != 0 ? vxlan.getVlan() : vxlan.getVirtualNetworkId());
            struct.setPhysicalInterface(phyNic);
            result.put(vxlan.getUuid(), struct);
        }
        return result;
    }
}
