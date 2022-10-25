package org.zstack.sdnController;

import org.zstack.header.core.Completion;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l3.*;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.sdnController.header.*;

import java.util.List;

public interface SdnController {
    /*
    有关sdn控制器的前置检查: pre-event
    对sdn控制器的控制: event
    有关sdn控制器的后置处理: post-event
     */
    void preInitSdnController(APIAddSdnControllerMsg msg, Completion completion);
    void initSdnController(APIAddSdnControllerMsg msg, Completion completion);
    void postInitSdnController(APIAddSdnControllerMsg msg, Completion completion);

    void preCreateVxlanNetwork(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);
    void createVxlanNetwork(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);
    void postCreateVxlanNetwork(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);

    void preAttachL2NetworkToCluster(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);
    void attachL2NetworkToCluster(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);
    void postAttachL2NetworkToCluster(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);

    void deleteSdnController(SdnControllerDeletionMsg msg, SdnControllerInventory sdn, Completion completion);
    void detachL2NetworkFromCluster(L2VxlanNetworkInventory vxlan, String clusterUuid, Completion completion);
    void deleteVxlanNetwork(L2VxlanNetworkInventory vxlan, Completion completion);

    List<SdnVniRange> getVniRange(SdnControllerInventory controller);
    List<SdnVlanRange> getVlanRange(SdnControllerInventory controller);
}
