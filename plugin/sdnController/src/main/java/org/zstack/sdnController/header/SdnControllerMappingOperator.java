package org.zstack.sdnController.header;

import org.zstack.core.db.Q;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;

import javax.persistence.Tuple;
import java.util.HashMap;
import java.util.Map;

public class SdnControllerMappingOperator {
    public static Map<Integer,String> getMappingVlanIdAndPhysicalInterfaceFromHost(L2VxlanNetworkInventory vxlan, String hostUuid) {
        Tuple hostMapping = Q.New(VxlanHostMappingVO.class)
                .select(VxlanHostMappingVO_.vlanId, VxlanHostMappingVO_.physicalInterface)
                .eq(VxlanHostMappingVO_.vxlanUuid, vxlan.getUuid())
                .eq(VxlanHostMappingVO_.hostUuid, hostUuid)
                .findTuple();
        Map<Integer,String> map = new HashMap<>();
        map.put(hostMapping.get(0, Integer.class), hostMapping.get(1, String.class));
        return map;
    }

    public static Map<Integer,String> getMappingVlanIdAndPhysicalInterfaceFromCluster(L2VxlanNetworkInventory vxlan, String clusterUuid) {
        Tuple clusterMapping = Q.New(VxlanClusterMappingVO.class)
                .select(VxlanClusterMappingVO_.vlanId, VxlanClusterMappingVO_.physicalInterface)
                .eq(VxlanClusterMappingVO_.vxlanUuid, vxlan.getUuid())
                .eq(VxlanClusterMappingVO_.clusterUuid, clusterUuid)
                .findTuple();
        Map<Integer,String> map = new HashMap<>();
        map.put(clusterMapping.get(0, Integer.class), clusterMapping.get(1, String.class));
        return map;
    }
}
