package org.zstack.sdnController;

import org.zstack.header.network.NetworkOperationContext;
import org.zstack.header.core.Completion;
import org.zstack.header.host.HostInventory;
import org.zstack.header.network.l2.APICreateL2NetworkMsg;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.header.network.sdncontroller.SdnControllerDeletionMsg;
import org.zstack.header.network.sdncontroller.SdnControllerInventory;
import org.zstack.sdnController.header.SdnVlanRange;
import org.zstack.sdnController.header.SdnVniRange;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SdnControllerL2 {
    void preCreateVxlanNetwork(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);
    void createL2Network(L2NetworkInventory inv, APICreateL2NetworkMsg msg, Completion completion);

    default void createL2Network(L2NetworkInventory inv, APICreateL2NetworkMsg msg,
                                 NetworkOperationContext operation, Completion completion) {
        createL2Network(inv, msg, completion);
    }

    default void beforeCommitL2NetworkInDb(L2NetworkInventory l2, NetworkOperationContext operation) {
    }

    default void beforeCommitL2NetworkInDb(L2NetworkInventory inv, APICreateL2NetworkMsg msg,
                                           NetworkOperationContext operation) {
        beforeCommitL2NetworkInDb(inv, operation);
    }

    default void afterL2NetworkFinally(L2NetworkInventory l2, NetworkOperationContext operation) {
    }

    default void afterL2NetworkFinally(L2NetworkInventory inv, APICreateL2NetworkMsg msg,
                                       NetworkOperationContext operation) {
        afterL2NetworkFinally(inv, operation);
    }

    default void updateL2Network(L2NetworkInventory l2, NetworkOperationContext operation, Completion completion) {
        completion.success();
    }

    default void beforeCommitUpdateL2NetworkInDb(L2NetworkInventory l2, NetworkOperationContext operation) {
    }

    void postCreateVxlanNetwork(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);

    void preAttachL2NetworkToCluster(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);
    void attachL2NetworkToCluster(L2VxlanNetworkInventory vxlan, List<String> clusterUuids, List<String> systemTags, Completion completion);
    default void attachL2NetworkToHosts(L2VxlanNetworkInventory vxlan, List<HostInventory> hinvs, List<String> systemTags, Completion completion) {completion.success();};
    void postAttachL2NetworkToCluster(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion);

    void deleteSdnController(SdnControllerDeletionMsg msg, SdnControllerInventory sdn, Completion completion);
    void detachL2NetworkFromCluster(L2VxlanNetworkInventory vxlan, List<String> clusterUuids, Completion completion);
    default void preDeleteL2Network(L2NetworkInventory inv) {
    }
    void deleteL2Network(L2NetworkInventory inv, Completion completion);

    default boolean isDeleteFailureBlocking() {
        return false;
    }

    default void afterDeleteL2Network(L2NetworkInventory inv) {
    }

    default void prepareL2NetworkForCluster(L2NetworkInventory network, String clusterUuid,
                                           Completion completion) {
        completion.success();
    }

    default void prepareL2NetworkForCluster(L2NetworkInventory network, String clusterUuid,
                                           NetworkOperationContext operation, Completion completion) {
        prepareL2NetworkForCluster(network, clusterUuid, completion);
    }

    default void beforeCommitAttachClusterInDb(L2NetworkInventory l2, String clusterUuid,
                                              NetworkOperationContext operation) {
    }

    default void beforeCommitAttachL2NetworkToClusterInDb(L2NetworkInventory network, String clusterUuid,
                                                         NetworkOperationContext operation) {
        beforeCommitAttachClusterInDb(network, clusterUuid, operation);
    }

    default void afterAttachL2NetworkToClusterFinally(L2NetworkInventory network, String clusterUuid,
                                                      NetworkOperationContext operation) {
        afterL2NetworkFinally(network, operation);
    }

    List<SdnVniRange> getVniRange(SdnControllerInventory controller);
    List<SdnVlanRange> getVlanRange(SdnControllerInventory controller);

    default List<Tuple> getL2NetworkOfSdnController() { return new ArrayList<>();};

    default void addVmNics(List<VmNicInventory> nics, Completion completion) {completion.success();};
    default void addVmNics(List<VmNicInventory> nics, VmInstanceSpec spec, Completion completion) {
        addVmNics(nics, completion);
    }
    /** Called before Start allocation; returned pre-existing ports are not owned by its rollback. */
    default Set<String> getVmNicUuidsToPreserveOnStartRollback(List<VmNicInventory> nics) {
        return Collections.emptySet();
    }
    default void removeVmNics(List<VmNicInventory> nics, Completion completion) {completion.success();};
    default void releaseNicIps(List<VmNicInventory> nics, Completion completion) {completion.success();};

    default void addL3NetworkIpRange(L3NetworkInventory inv, IpRangeInventory ipr, Completion completion) {completion.success();};
    default void deleteL3NetworkIpRange(L3NetworkInventory inv, IpRangeInventory ipr, Completion completion) {completion.success();};
}
