package org.zstack.sdnController;

import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostInventory;
import org.zstack.header.network.l2.APICreateL2NetworkMsg;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.NetworkCreateContext;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.header.network.sdncontroller.SdnControllerDeletionMsg;
import org.zstack.header.network.sdncontroller.SdnControllerInventory;
import org.zstack.sdnController.header.SdnVlanRange;
import org.zstack.sdnController.header.SdnVniRange;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.List;

public interface SdnControllerL2 {
    void preCreateVxlanNetwork(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);
    void createL2Network(L2NetworkInventory inv, APICreateL2NetworkMsg msg, Completion completion);
    default void createL2Network(L2NetworkInventory inv, APICreateL2NetworkMsg msg, NetworkCreateContext context, Completion completion) {
        createL2Network(inv, msg, completion);
    }
    void postCreateVxlanNetwork(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);

    void preAttachL2NetworkToCluster(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);
    void attachL2NetworkToCluster(L2VxlanNetworkInventory vxlan, List<String> clusterUuids, List<String> systemTags, Completion completion);
    default void attachL2NetworkToHosts(L2VxlanNetworkInventory vxlan, List<HostInventory> hinvs, List<String> systemTags, Completion completion) {completion.success();};
    void postAttachL2NetworkToCluster(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);

    void deleteSdnController(SdnControllerDeletionMsg msg, SdnControllerInventory sdn, Completion completion);
    void detachL2NetworkFromCluster(L2VxlanNetworkInventory vxlan, List<String> clusterUuids, Completion completion);
    void deleteL2Network(L2NetworkInventory inv, Completion completion);
    default boolean requiresConfirmedDelete() { return false; }
    default void deleteL2Network(L2NetworkInventory inv, String operationUuid, Completion completion) {
        deleteL2Network(inv, completion);
    }
    default ErrorCode beginConfirmedDelete(L2NetworkInventory inv) { return null; }
    default ErrorCode checkConfirmedDelete(L2NetworkInventory inv) { return null; }
    default ErrorCode completeConfirmedDelete(L2NetworkInventory inv) { return null; }
    default ErrorCode cancelConfirmedDelete(L2NetworkInventory inv) { return null; }
    default void deleteConfirmedLocalMetadata(L2NetworkInventory inv) { }

    List<SdnVniRange> getVniRange(SdnControllerInventory controller);
    List<SdnVlanRange> getVlanRange(SdnControllerInventory controller);

    default List<Tuple> getL2NetworkOfSdnController() { return new ArrayList<>();};

    default void addVmNics(List<VmNicInventory> nics, Completion completion) {completion.success();};
    default void removeVmNics(List<VmNicInventory> nics, Completion completion) {completion.success();};
    default void releaseNicIps(List<VmNicInventory> nics, Completion completion) {completion.success();};

    default void addL3NetworkIpRange(L3NetworkInventory inv, IpRangeInventory ipr, Completion completion) {completion.success();};
    default void deleteL3NetworkIpRange(L3NetworkInventory inv, IpRangeInventory ipr, Completion completion) {completion.success();};
}
