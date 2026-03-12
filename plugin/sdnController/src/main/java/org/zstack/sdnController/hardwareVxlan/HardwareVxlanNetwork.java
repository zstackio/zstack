package org.zstack.sdnController.hardwareVxlan;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.network.l2.APICreateL2NetworkMsg;
import org.zstack.header.network.l2.DetachL2NetworkFromClusterMsg;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkClusterRefVO_;
import org.zstack.header.network.l2.L2NetworkRealizationExtensionPoint;
import org.zstack.header.network.l2.L2NetworkType;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.VSwitchType;
import org.zstack.header.network.sdncontroller.SdnControllerConstant;
import org.zstack.sdnController.header.HardwareL2VxlanNetworkPoolVO;
import org.zstack.header.network.sdncontroller.SdnControllerVO;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetwork;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;
import org.zstack.sdnController.SdnControllerL2;
import org.zstack.sdnController.SdnControllerManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

/**
 * Created by shixin.ruan on 09/19/2019.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HardwareVxlanNetwork extends VxlanNetwork implements HardwareVxlanNetworkExtensionPoint {
    @Autowired
    SdnControllerManager sdnControllerManager;

    private static final CLogger logger = Utils.getLogger(HardwareVxlanNetwork.class);

    public HardwareVxlanNetwork(L2NetworkVO self) {
        super(self);
    }

    @Override
    public void createVxlanNetworkOnSdnController(L2VxlanNetworkInventory vxlan, APICreateL2NetworkMsg msg, Completion completion) {
        HardwareL2VxlanNetworkPoolVO poolVO = dbf.findByUuid(vxlan.getPoolUuid(), HardwareL2VxlanNetworkPoolVO.class);
        SdnControllerVO sdn = dbf.findByUuid(poolVO.getSdnControllerUuid(), SdnControllerVO.class);
        SdnControllerL2 sdnController = sdnControllerManager.getSdnControllerL2(sdn);
        sdnController.createL2Network(vxlan, msg, completion);
    }

    @Override
    public void deleteVxlanNetworkOnSdnController(VxlanNetworkVO vo, Completion completion) {
        HardwareL2VxlanNetworkPoolVO poolVO = dbf.findByUuid(vo.getPoolUuid(), HardwareL2VxlanNetworkPoolVO.class);
        if (poolVO == null || poolVO.getSdnControllerUuid() == null) {
            completion.fail(argerr(ORG_ZSTACK_SDNCONTROLLER_HARDWAREVXLAN_10000, "there is no sdn controller for vxlan pool [uuid:%s]", vo.getPoolUuid()));
            return;
        }

        SdnControllerVO sdn = dbf.findByUuid(poolVO.getSdnControllerUuid(), SdnControllerVO.class);
        SdnControllerL2 sdnController = sdnControllerManager.getSdnControllerL2(sdn);
        sdnController.deleteL2Network(L2VxlanNetworkInventory.valueOf(vo), completion);
    }

    private void realizeNetwork(String hostUuid, String htype, L2VxlanNetworkInventory inv , Completion completion) {
        final HypervisorType hvType = HypervisorType.valueOf(htype);
        final L2NetworkType l2Type = L2NetworkType.valueOf(SdnControllerConstant.HARDWARE_VXLAN_NETWORK_TYPE);
        final VSwitchType vSwitchType = VSwitchType.valueOf(inv.getvSwitchType());

        L2NetworkRealizationExtensionPoint ext = l2Mgr.getRealizationExtension(l2Type, vSwitchType, hvType);
        ext.realize(inv, hostUuid, completion);
    }

    public void attachL2NetworkToHosts(L2VxlanNetworkInventory vxlan, List<HostInventory> hvinvs, Completion completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("attach-hardware-vxlan-%s-on-hosts", vxlan.getUuid()));
        // DEBT: NoRollbackFlow — in attachL2NetworkToHosts
        chain.then(new NoRollbackFlow() {
            final String __name__ = "realize-physical-interface";

            private void realize(final Iterator<HostInventory> it, final FlowTrigger trigger) {
                if (!it.hasNext()) {
                    trigger.next();
                    return;
                }

                HostInventory host = it.next();
                realizeNetwork(host.getUuid(), host.getHypervisorType(), vxlan, new Completion(trigger) {
                    @Override
                    public void success() {
                        realize(it, trigger);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                realize(hvinvs.iterator(), trigger);
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    @Override
    public void attachL2NetworkToCluster(L2VxlanNetworkInventory vxlan, APICreateL2NetworkMsg msg, Completion completion) {
        List<String> clusterUuids = Q.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.l2NetworkUuid, vxlan.getPoolUuid())
                .select(L2NetworkClusterRefVO_.clusterUuid).listValues();
        if (clusterUuids == null || clusterUuids.isEmpty()) {
            logger.debug(String.format("no cluster attached to hardware vxlan network[uuid:%s, name:%s]",
                    vxlan.getUuid(), vxlan.getName()));
            completion.success();
            return;
        }

        List<HostVO> hosts = Q.New(HostVO.class).in(HostVO_.clusterUuid, clusterUuids)
                .notIn(HostVO_.state, asList(HostState.PreMaintenance, HostState.Maintenance))
                .eq(HostVO_.status, HostStatus.Connected).list();
        List<HostInventory> hvinvs = HostInventory.valueOf(hosts);

        attachL2NetworkToHosts(vxlan, hvinvs, completion);
    }

    @Override
    public void deleteHook(Completion completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-hardware-vxlan"));
        // DEBT: NoRollbackFlow — in deleteHook
        chain.then(new NoRollbackFlow() {
            final String __name__ = "delete-hardware-vxlan-from-sdn";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                deleteVxlanNetworkOnSdnController((VxlanNetworkVO) self, new Completion(completion) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.next();
                    }
                });
            }
        // DEBT: NoRollbackFlow — in deleteHook
        }).then(new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                HardwareVxlanNetwork.super.deleteL2Bridge(null, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }

    // called by handle(DetachL2NetworkFromClusterMsg msg)
    @Override
    protected void deleteL2Bridge(List<String> clusterUuids, Completion completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("detach-hardware-vxlan"));
        // DEBT: NoRollbackFlow — in deleteL2Bridge
        chain.then(new NoRollbackFlow() {
            final String __name__ = "detach-hardware-vxlan-from-sdn";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                L2VxlanNetworkInventory vxlan = getSelfInventory();
                HardwareL2VxlanNetworkPoolVO poolVO = dbf.findByUuid(vxlan.getPoolUuid(), HardwareL2VxlanNetworkPoolVO.class);
                SdnControllerVO sdn = dbf.findByUuid(poolVO.getSdnControllerUuid(), SdnControllerVO.class);

                SdnControllerL2 controller = sdnControllerManager.getSdnControllerL2(sdn);
                controller.detachL2NetworkFromCluster(vxlan, clusterUuids, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.next();
                    }
                });
            }
        // DEBT: NoRollbackFlow — in deleteL2Bridge
        }).then(new NoRollbackFlow() {
            final String __name__ = "detach-hardware-vxlan-from-host";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                HardwareVxlanNetwork.super.deleteL2Bridge(clusterUuids, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.next();
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                completion.fail(errCode);
            }
        }).start();
    }
}
