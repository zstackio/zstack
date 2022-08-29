package org.zstack.sdnController.hardwareVxlan;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.*;
import org.zstack.header.network.l2.*;
import org.zstack.network.l2.vxlan.vxlanNetwork.*;
import org.zstack.sdnController.SdnController;
import org.zstack.sdnController.SdnControllerManager;
import org.zstack.sdnController.header.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;

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
    public void createVxlanNetworkOnSdnController(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion) {
        HardwareL2VxlanNetworkPoolVO poolVO = dbf.findByUuid(vxlan.getPoolUuid(), HardwareL2VxlanNetworkPoolVO.class);
        SdnControllerVO sdn = dbf.findByUuid(poolVO.getSdnControllerUuid(), SdnControllerVO.class);
        SdnController sdnController = sdnControllerManager.getSdnController(sdn);
        sdnController.createVxlanNetwork(vxlan, systemTags, completion);
    }

    @Override
    public void deleteVxlanNetworkOnSdnController(VxlanNetworkVO vo, Completion completion) {
        HardwareL2VxlanNetworkPoolVO poolVO = dbf.findByUuid(vo.getPoolUuid(), HardwareL2VxlanNetworkPoolVO.class);
        if (poolVO == null || poolVO.getSdnControllerUuid() == null) {
            completion.fail(argerr("there is no sdn controller for vxlan pool [uuid:%s]", vo.getPoolUuid()));
            return;
        }

        SdnControllerVO sdn = dbf.findByUuid(poolVO.getSdnControllerUuid(), SdnControllerVO.class);
        SdnController sdnController = sdnControllerManager.getSdnController(sdn);
        sdnController.deleteVxlanNetwork(L2VxlanNetworkInventory.valueOf(vo), completion);
    }

    private void realizeNetwork(String hostUuid, String htype, L2VxlanNetworkInventory inv , Completion completion) {
        final HypervisorType hvType = HypervisorType.valueOf(htype);
        final L2NetworkType l2Type = L2NetworkType.valueOf(SdnControllerConstant.HARDWARE_VXLAN_NETWORK_TYPE);

        L2NetworkRealizationExtensionPoint ext = l2Mgr.getRealizationExtension(l2Type, hvType);
        ext.realize(inv, hostUuid, completion);
    }

    @Override
    public void attachL2NetworkToClusterOnSdnController(L2VxlanNetworkInventory vxlan, List<String> systemTags, Completion completion) {
        List<String> clusterUuids = Q.New(L2NetworkClusterRefVO.class).eq(L2NetworkClusterRefVO_.l2NetworkUuid, vxlan.getPoolUuid())
                .select(L2NetworkClusterRefVO_.clusterUuid).listValues();
        if (clusterUuids == null || clusterUuids.isEmpty()) {
            completion.success();
            return;
        }

        List<HostVO> hosts = Q.New(HostVO.class).in(HostVO_.clusterUuid, clusterUuids)
                .notIn(HostVO_.state, asList(HostState.PreMaintenance, HostState.Maintenance))
                .eq(HostVO_.status, HostStatus.Connected).list();
        List<HostInventory> hvinvs = HostInventory.valueOf(hosts);

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("attach-hardware-vxlan-%s-on-hosts", vxlan.getUuid()));
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
    public Integer getMappingVxlanId(String hostUuid) {
        VxlanNetworkVO vo = (VxlanNetworkVO) self;
        HardwareL2VxlanNetworkPoolVO poolVO = dbf.findByUuid(vo.getPoolUuid(), HardwareL2VxlanNetworkPoolVO.class);
        SdnControllerVO sdnVo = dbf.findByUuid(poolVO.getSdnControllerUuid(), SdnControllerVO.class);

        SdnController sdn = sdnControllerManager.getSdnController(sdnVo);
        return sdn.getMappingVlanId(L2VxlanNetworkInventory.valueOf((VxlanNetworkVO)self), hostUuid);
    }

    @Override
    public Map<Integer, String> getMappingVlanIdAndPhysicalInterfaceFromHost(L2VxlanNetworkInventory vxlan, String hostUuid) {
        VxlanNetworkVO vo = (VxlanNetworkVO) self;
        HardwareL2VxlanNetworkPoolVO poolVO = dbf.findByUuid(vo.getPoolUuid(), HardwareL2VxlanNetworkPoolVO.class);
        SdnControllerVO sdnVo = dbf.findByUuid(poolVO.getSdnControllerUuid(), SdnControllerVO.class);

        SdnController sdn = sdnControllerManager.getSdnController(sdnVo);
        return sdn.getMappingVlanIdAndPhysicalInterfaceFromHost(L2VxlanNetworkInventory.valueOf((VxlanNetworkVO)self), hostUuid);
    }

    @Override
    public Map<Integer, String> getMappingVlanIdAndPhysicalInterfaceFromCluster(L2VxlanNetworkInventory vxlan, String clusterUuid) {
        VxlanNetworkVO vo = (VxlanNetworkVO) self;
        HardwareL2VxlanNetworkPoolVO poolVO = dbf.findByUuid(vo.getPoolUuid(), HardwareL2VxlanNetworkPoolVO.class);
        SdnControllerVO sdnVo = dbf.findByUuid(poolVO.getSdnControllerUuid(), SdnControllerVO.class);

        SdnController sdn = sdnControllerManager.getSdnController(sdnVo);
        return sdn.getMappingVlanIdAndPhysicalInterfaceFromCluster(L2VxlanNetworkInventory.valueOf((VxlanNetworkVO)self), clusterUuid);
    }

    @Override
    public void deleteHook(Completion completion) {
        deleteVxlanNetworkOnSdnController((VxlanNetworkVO) self, new Completion(completion) {
            @Override
            public void success() {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.success();
            }
        });
    }
}
