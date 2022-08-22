package org.zstack.sdnController;

import org.zstack.header.core.Completion;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.sdnController.header.*;

import java.util.List;
import java.util.Map;

public interface SdnController {
//    void initSdnController(APIAddSdnControllerMsg msg, Completion completion);
//    void createVxlanNetwork(L2VxlanNetworkInventory vxlan, Completion completion);
//    void deleteVxlanNetwork(L2VxlanNetworkInventory vxlan, Completion completion);
//    int  getMappingVlanId(L2VxlanNetworkInventory vxlan, String hostUuid);

    /*
    有关sdn控制器的前置检查: pre-event
    对sdn控制器的控制:post-event
     */
    void preInitSdnController(SdnControllerInventory controller, List<String> systemTags, Completion completion);
    void postInitSdnController(APIAddSdnControllerMsg msg, Completion completion);

//TODO lldp获取sw和sw port数据  sw + sw port + vlan + vni
//    void createAccessPort(L2VxlanNetworkInventory l2Network, HostInventory host, Completion completion);
//    void deleteAccessPort(L2VxlanNetworkInventory l2Network, HostInventory host, Completion completion);
    int  getMappingVlanId(L2VxlanNetworkInventory vxlan, String hostUuid);
    Map<Integer, String> getMappingVlanIdAndPhysicalInterface(L2VxlanNetworkInventory vxlan, String hostUuid);

    void preCreateVxlanNetworkPool(HardwareL2VxlanNetworkPoolInventory pool, List<String> systemTags, Completion completion);
    void postCreateVxlanNetworkPool(HardwareL2VxlanNetworkPoolInventory pool, List<String> systemTags, Completion completion);
    void preCreateVxlanNetwork(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);
    void postCreateVxlanNetwork(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);
    void preAttachL2NetworkToCluster(L2VxlanNetworkInventory vxlan, String clusterUuid, List<String> systemTags, Completion completion);
    void postAttachL2NetworkToCluster(L2VxlanNetworkInventory vxlan, String clusterUuid, List<String> systemTags, Completion completion);

    void preDeleteSdnController(SdnControllerInventory controller, Completion completion);
    void postDeleteSdnController(SdnControllerInventory controller, Completion completion);
    void preDetachL2NetworkFromCluster(L2VxlanNetworkInventory vxlan, String clusterUuid, Completion completion);
    void postDetachL2NetworkFromCluster(L2VxlanNetworkInventory vxlan, String clusterUuid, Completion completion);
    void preDeleteVxlanNetwork(L2VxlanNetworkInventory vxlan, Completion completion);
    void postDeleteVxlanNetwork(L2VxlanNetworkInventory vxlan, Completion completion);

    List<SdnVniRange> getVniRange(SdnControllerInventory controller);
    List<SdnVlanRange> getAccessVlanRange(SdnControllerInventory controller);
}
