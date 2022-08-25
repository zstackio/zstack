package org.zstack.sdnController;

import org.zstack.header.core.Completion;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.sdnController.header.*;

import java.util.List;
import java.util.Map;

public interface SdnController {
    /*
    有关sdn控制器的前置检查: pre-event
    对sdn控制器的控制: event
    有关sdn控制器的后置处理: post-event
     */
    void preInitSdnController(APIAddSdnControllerMsg msg, SdnControllerInventory sdn, Completion completion);
    void initSdnController(APIAddSdnControllerMsg msg, SdnControllerInventory sdn, Completion completion);
    void postInitSdnController(APIAddSdnControllerMsg msg, SdnControllerInventory sdn, Completion completion);

    void preCreateVxlanNetwork(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);
    void createVxlanNetwork(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);
    void postCreateVxlanNetwork(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);

    void preAttachL2NetworkToCluster(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);
    void attachL2NetworkToCluster(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);
    void postAttachL2NetworkToCluster(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);

    void deleteSdnController(SdnControllerDeletionMsg msg, SdnControllerInventory sdn, Completion completion);
    void detachL2NetworkFromCluster(L2VxlanNetworkInventory vxlan, String clusterUuid, Completion completion);
    void deleteVxlanNetwork(L2VxlanNetworkInventory vxlan, Completion completion);

    //TODO lldp获取sw和sw port数据  sw + sw port + vlan + vni
    int  getMappingVlanId(L2VxlanNetworkInventory vxlan, String hostUuid);
    Map<Integer, String> getMappingVlanIdAndPhysicalInterfaceFromHost(L2VxlanNetworkInventory vxlan, String hostUuid);
    Map<Integer, String> getMappingVlanIdAndPhysicalInterfaceFromCluster(L2VxlanNetworkInventory vxlan, String clusterUuid);

    List<SdnVniRange> getVniRange(SdnControllerInventory controller);
    List<SdnVlanRange> getAccessVlanRange(SdnControllerInventory controller);
}
