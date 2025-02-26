package org.zstack.sdnController;

import org.zstack.header.core.Completion;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.sdnController.header.SdnControllerDeletionMsg;
import org.zstack.sdnController.header.SdnControllerInventory;
import org.zstack.sdnController.header.SdnVlanRange;
import org.zstack.sdnController.header.SdnVniRange;

import java.util.ArrayList;
import java.util.List;

public interface SdnControllerL2 {
    void preCreateVxlanNetwork(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);
    void createL2Network(L2NetworkInventory inv, List<String> systemTags, Completion completion);
    void postCreateVxlanNetwork(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);

    void preAttachL2NetworkToCluster(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);
    void attachL2NetworkToCluster(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);
    void postAttachL2NetworkToCluster(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);

    void deleteSdnController(SdnControllerDeletionMsg msg, SdnControllerInventory sdn, Completion completion);
    void detachL2NetworkFromCluster(L2VxlanNetworkInventory vxlan, String clusterUuid, Completion completion);
    void deleteL2Network(L2NetworkInventory inv, Completion completion);

    List<SdnVniRange> getVniRange(SdnControllerInventory controller);
    List<SdnVlanRange> getVlanRange(SdnControllerInventory controller);

    default List<String> getL2NetworkOfSdnController() { return new ArrayList<>();};

    default void addVmNics(List<VmNicInventory> nics, Completion completion) {completion.success();};
    default void removeVmNics(List<VmNicInventory> nics, Completion completion) {completion.success();};
}
