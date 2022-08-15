package org.zstack.sdnController.header;

import org.zstack.core.db.Q;
import org.zstack.header.host.HostInventory;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;

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

    public static VxlanHostMappingStruct getHardwareVxlanMappingVxlanId(L2VxlanNetworkInventory vxlan, HostInventory host) {
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

        struct.setVlanId(vxlan.getVni());
        struct.setPhysicalInterface(phyNic);
        return struct;
    }
}
