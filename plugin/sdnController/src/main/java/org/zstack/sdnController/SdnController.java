package org.zstack.sdnController;

import org.zstack.header.core.Completion;
import org.zstack.header.host.HostInventory;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.L2VxlanNetworkPoolInventory;
import org.zstack.sdnController.header.*;

import java.util.List;

public interface SdnController {
//    void initSdnController(APIAddSdnControllerMsg msg, Completion completion);
//    void createVxlanNetwork(L2VxlanNetworkInventory vxlan, Completion completion);
//    void deleteVxlanNetwork(L2VxlanNetworkInventory vxlan, Completion completion);
//    int  getMappingVlanId(L2VxlanNetworkInventory vxlan, String hostUuid);

//    void preCreateSdnController(SdnControllerInventory controller, List<String> systemTags, Completion completion);
    void initSdnController(APIAddSdnControllerMsg msg, Completion completion);
//    void postCreateSdnController(SdnControllerInventory controller, List<String> systemTags, Completion completion);

    // TODO lldp获取sw和sw port数据  sw + sw port + vlan + vni
//    void createAccessPort(L2VxlanNetworkInventory l2Network, HostInventory host, Completion completion);
//    void deleteAccessPort(L2VxlanNetworkInventory l2Network, HostInventory host, Completion completion);
    // 获取host对应的vlanId
    // H3c
    int  getMappingVlanId(L2VxlanNetworkInventory vxlan, String hostUuid);

    void preCreateVxlanNetworkPool(HardwareL2VxlanNetworkPoolInventory pool, List<String> systemTags, Completion completion);
    void postCreateVxlanNetworkPool(HardwareL2VxlanNetworkPoolInventory pool, List<String> systemTags, Completion completion);

//    void preCreateVxlanNetwork(L2VxlanNetworkInventory vxlan);
    void createVxlanNetwork(L2VxlanNetworkInventory vxlan, Completion completion);
//    void postCreateVxlanNetwork(L2VxlanNetworkInventory l2Network, Completion completion);
//    void preCreateL2Network(L2NetworkInventory l2Network, List<String> systemTags, Completion completion);
//    void postCreateL2Network(L2NetworkInventory l2Network, List<String> systemTags, Completion completion);
//    void preAttachL2NetworkToCluster(L2NetworkInventory l2Network, String clusterUuid, List<String> systemTags, Completion completion);
//    void postAttachL2NetworkToCluster(L2NetworkInventory l2Network, String clusterUuid, List<String> systemTags, Completion completion);

//    void preDeleteVxlanNetwork(L2VxlanNetworkInventory vxlan);
    void deleteVxlanNetwork(L2VxlanNetworkInventory vxlan, Completion completion);
//    void postDeleteVxlanNetwork(L2VxlanNetworkInventory vxlan, Completion completion);
//    void preDeleteSdnController(SdnControllerInventory controller, Completion completion);
//    void postDeleteSdnController(SdnControllerInventory controller, Completion completion);
//    void preDetachL2NetworkToCluster(L2NetworkInventory l2Network, String clusterUuid, Completion completion);
//    void postDetachL2NetworkToCluster(L2NetworkInventory l2Network, String clusterUuid, Completion completion);
//    void preDeleteL2Network(L2NetworkInventory l2Network, Completion completion);
//    void postDeleteL2Network(L2NetworkInventory l2Network, Completion completion);



    // 获取全局配置
    List<SdnVniRange> getVniRange(SdnControllerInventory controller);
    List<SdnVlanRange> getAccessVlanRange(SdnControllerInventory controller);
}
