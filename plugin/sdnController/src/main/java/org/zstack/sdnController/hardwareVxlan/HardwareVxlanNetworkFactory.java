package org.zstack.sdnController.hardwareVxlan;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.*;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceMigrateExtensionPoint;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.network.l2.L2NetworkCascadeFilterExtensionPoint;
import org.zstack.network.l2.L2NetworkDefaultMtu;
import org.zstack.network.l2.L2NetworkManager;
import org.zstack.network.l2.vxlan.vxlanNetwork.*;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.AllocateVniMsg;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.AllocateVniReply;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolVO;
import org.zstack.network.service.NetworkServiceGlobalConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.sdnController.SdnController;
import org.zstack.sdnController.SdnControllerManager;
import org.zstack.sdnController.header.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

/**
 * Created by shixin.ruan on 09/17/2019.
 */
public class HardwareVxlanNetworkFactory implements L2NetworkFactory, VmInstanceMigrateExtensionPoint, L2NetworkDefaultMtu, L2NetworkGetVniExtensionPoint, L2NetworkCascadeFilterExtensionPoint {
    private static CLogger logger = Utils.getLogger(HardwareVxlanNetworkFactory.class);
    public static L2NetworkType type = new L2NetworkType(SdnControllerConstant.HARDWARE_VXLAN_NETWORK_TYPE);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private SdnControllerManager sdnControllerManager;
    @Autowired
    private L2NetworkManager l2Mgr;
    @Autowired
    private ResourceConfigFacade rcf;

    @Override
    public L2NetworkType getType() {
        return type;
    }

    @Override
    public void createL2Network(L2NetworkVO ovo, APICreateL2NetworkMsg msg, ReturnValueCompletion completion) {
        APICreateL2HardwareVxlanNetworkMsg amsg = (APICreateL2HardwareVxlanNetworkMsg) msg;
        VxlanNetworkVO vo = new VxlanNetworkVO(ovo);
        vo.setAccountUuid(msg.getSession().getAccountUuid());
        vo.setPoolUuid((amsg.getPoolUuid()));
        VxlanNetworkPoolVO poolVO = dbf.findByUuid(vo.getPoolUuid(), VxlanNetworkPoolVO.class);
        vo.setPhysicalInterface(poolVO.getPhysicalInterface());
        vo.setVni(0);

        HardwareVxlanNetwork hardwareVxlan = new HardwareVxlanNetwork(vo);

        Map data = new HashMap();
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setData(data);
        chain.setName(String.format("create-hardware-vxlan-network-%s", msg.getName()));
        chain.then(new ShareFlow(){
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = String.format("pre-process-for-create-hardware-vxlan-network-%s", msg.getName());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        L2VxlanNetworkInventory vxlan = L2VxlanNetworkInventory.valueOf(vo);
                        HardwareL2VxlanNetworkPoolVO poolVO = dbf.findByUuid(vxlan.getPoolUuid(), HardwareL2VxlanNetworkPoolVO.class);
                        if (poolVO == null || poolVO.getSdnControllerUuid() == null) {
                            completion.fail(argerr("there is no sdn controller for vxlan pool [uuid:%s]", vxlan.getPoolUuid()));
                            return;
                        }
                        SdnControllerVO sdn = dbf.findByUuid(poolVO.getSdnControllerUuid(), SdnControllerVO.class);
                        SdnController controller = sdnControllerManager.getSdnController(sdn);

                        controller.preCreateVxlanNetwork(vxlan, msg.getSystemTags(), new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });
                flow(new Flow() {
                    String __name__ = String.format("allocate-vni-for-%s-and-store-in-db", msg.getName());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        AllocateVniMsg vniMsg = new AllocateVniMsg();
                        vniMsg.setL2NetworkUuid(amsg.getPoolUuid());
                        vniMsg.setRequiredVni(amsg.getVni());
                        bus.makeTargetServiceIdByResourceUuid(vniMsg, L2NetworkConstant.SERVICE_ID, amsg.getPoolUuid());
                        bus.send(vniMsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                AllocateVniReply r = reply.castReply();
                                vo.setVni(r.getVni());
                                dbf.persist(vo);

                                data.put(SdnControllerConstant.Params.VXLAN_NETWORK.toString(), vo);
                                trigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        /* no need to release vni because vni is saved in VxlanNetworkVO */
                        trigger.rollback();
                    }
                });
                flow(new Flow() {
                    String __name__ = String.format("create-hardware-vxlan-on-sdn-controller-%s", msg.getName());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VxlanNetworkVO ovo = (VxlanNetworkVO)data.get(SdnControllerConstant.Params.VXLAN_NETWORK.toString());
                        L2VxlanNetworkInventory vxlan = L2VxlanNetworkInventory.valueOf(ovo);
                        hardwareVxlan.createVxlanNetworkOnSdnController(vxlan, msg.getSystemTags(), new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        VxlanNetworkVO ovo = (VxlanNetworkVO)data.get(SdnControllerConstant.Params.VXLAN_NETWORK.toString());
                        hardwareVxlan.deleteVxlanNetworkOnSdnController(ovo, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.rollback();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.rollback();
                            }
                        });
                    }
                });
                flow(new NoRollbackFlow() {
                    String __name__ = String.format("post-process-for-create-sdn-controller--%s", msg.getName());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VxlanNetworkVO ovo = (VxlanNetworkVO)data.get(SdnControllerConstant.Params.VXLAN_NETWORK.toString());
                        L2VxlanNetworkInventory vxlan = L2VxlanNetworkInventory.valueOf(ovo);
                        HardwareL2VxlanNetworkPoolVO poolVO = dbf.findByUuid(vxlan.getPoolUuid(), HardwareL2VxlanNetworkPoolVO.class);
                        SdnControllerVO sdn = dbf.findByUuid(poolVO.getSdnControllerUuid(), SdnControllerVO.class);

                        SdnController controller = sdnControllerManager.getSdnController(sdn);
                        controller.postCreateVxlanNetwork(vxlan, msg.getSystemTags(), new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });
                flow(new NoRollbackFlow() {
                    String __name__ = String.format("pre-process-for-attach-hardware-vxlan-on-host-%s", msg.getName());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VxlanNetworkVO ovo = (VxlanNetworkVO)data.get(SdnControllerConstant.Params.VXLAN_NETWORK.toString());
                        L2VxlanNetworkInventory vxlan = L2VxlanNetworkInventory.valueOf(ovo);
                        HardwareL2VxlanNetworkPoolVO poolVO = dbf.findByUuid(vxlan.getPoolUuid(), HardwareL2VxlanNetworkPoolVO.class);
                        SdnControllerVO sdn = dbf.findByUuid(poolVO.getSdnControllerUuid(), SdnControllerVO.class);

                        SdnController controller = sdnControllerManager.getSdnController(sdn);
                        controller.preAttachL2NetworkToCluster(vxlan, msg.getSystemTags(), new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });
                flow(new Flow() {
                    String __name__ = String.format("attach-hardware-vxlan-on-host-%s-and-store-in-db", msg.getName());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VxlanNetworkVO vov = (VxlanNetworkVO)data.get(SdnControllerConstant.Params.VXLAN_NETWORK.toString());
                        L2VxlanNetworkInventory vxlan = L2VxlanNetworkInventory.valueOf(vov);
                        hardwareVxlan.attachL2NetworkToClusterOnSdnController(vxlan, msg.getSystemTags(), new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        /* same to vlan network, when network is deleted, bridge is deleted from host */
                        trigger.rollback();
                    }
                });
                flow(new Flow() {
                    String __name__ = String.format("attach-hardware-vxlan-on-host-on-sdn-controller-%s", msg.getName());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VxlanNetworkVO ovo = (VxlanNetworkVO)data.get(SdnControllerConstant.Params.VXLAN_NETWORK.toString());
                        L2VxlanNetworkInventory vxlan = L2VxlanNetworkInventory.valueOf(ovo);
                        HardwareL2VxlanNetworkPoolVO poolVO = dbf.findByUuid(vxlan.getPoolUuid(), HardwareL2VxlanNetworkPoolVO.class);
                        SdnControllerVO sdn = dbf.findByUuid(poolVO.getSdnControllerUuid(), SdnControllerVO.class);

                        SdnController controller = sdnControllerManager.getSdnController(sdn);
                        controller.attachL2NetworkToCluster(vxlan, msg.getSystemTags(), new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        trigger.rollback();
                    }
                });
                flow(new NoRollbackFlow() {
                    String __name__ = String.format("post-process-for-attach-hardware-vxlan-on-host-%s", msg.getName());

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        VxlanNetworkVO ovo = (VxlanNetworkVO)data.get(SdnControllerConstant.Params.VXLAN_NETWORK.toString());
                        L2VxlanNetworkInventory vxlan = L2VxlanNetworkInventory.valueOf(ovo);
                        HardwareL2VxlanNetworkPoolVO poolVO = dbf.findByUuid(vxlan.getPoolUuid(), HardwareL2VxlanNetworkPoolVO.class);
                        SdnControllerVO sdn = dbf.findByUuid(poolVO.getSdnControllerUuid(), SdnControllerVO.class);

                        SdnController controller = sdnControllerManager.getSdnController(sdn);
                        controller.postAttachL2NetworkToCluster(vxlan, msg.getSystemTags(), new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        List<String> clusterUuids = Q.New(L2NetworkClusterRefVO.class)
                                .eq(L2NetworkClusterRefVO_.l2NetworkUuid, amsg.getPoolUuid())
                                .select(L2NetworkClusterRefVO_.clusterUuid).listValues();
                        if (clusterUuids != null && !clusterUuids.isEmpty()) {
                            List<L2NetworkClusterRefVO> refs = new ArrayList<>();
                            for (String cluster: clusterUuids) {
                                L2NetworkClusterRefVO ref = new L2NetworkClusterRefVO();
                                ref.setClusterUuid(cluster);
                                ref.setL2NetworkUuid(vo.getUuid());
                                refs.add(ref);
                            }

                            dbf.persistCollection(refs);
                        }

                        completion.success(L2VxlanNetworkInventory.valueOf(dbf.findByUuid(vo.getUuid(), VxlanNetworkVO.class)));
                    }
                });
                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        dbf.removeByPrimaryKey(vo.getUuid(), VxlanNetworkVO.class);
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
    }

    @Override
    public L2Network getL2Network(L2NetworkVO vo) {
        return new HardwareVxlanNetwork(vo);
    }

    @Override
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid) {
        List<VmNicInventory> nics = inv.getVmNics();
        List<L3NetworkVO> l3vos = new ArrayList<>();
        /* FIXME: shixin need add ipv6 on vlxan network */
        nics.stream().forEach((nic -> l3vos.add(Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid ,nic.getL3NetworkUuid()).find())));

        List<String> vxlanUuids = new ArrayList<>();
        for (L3NetworkVO l3 : l3vos) {
            String type = Q.New(L2NetworkVO.class).select(L2NetworkVO_.type).eq(L2NetworkVO_.uuid, l3.getL2NetworkUuid()).findValue();
            if (type.equals(SdnControllerConstant.HARDWARE_VXLAN_NETWORK_TYPE)) {
                vxlanUuids.add(l3.getL2NetworkUuid());
            }
        }

        if (vxlanUuids.isEmpty()) {
            return;
        }

        ErrorCodeList errList = new ErrorCodeList();
        FutureCompletion completion = new FutureCompletion(null);

        new While<>(vxlanUuids).all((uuid, completion1) -> {
            PrepareL2NetworkOnHostMsg msg = new PrepareL2NetworkOnHostMsg();
            msg.setL2NetworkUuid(uuid);
            msg.setHost(HostInventory.valueOf((HostVO) Q.New(HostVO.class).eq(HostVO_.uuid, destHostUuid).find()));
            bus.makeTargetServiceIdByResourceUuid(msg, L2NetworkConstant.SERVICE_ID, uuid);
            bus.send(msg, new CloudBusCallBack(completion1) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.warn(reply.getError().toString());
                        errList.getCauses().add(reply.getError());
                    } else {
                        logger.debug(String.format("check and realize hardware vxlan network[uuid: %s] for vm[uuid: %s] successed", uuid, inv.getUuid()));
                    }
                    completion1.done();

                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errList.getCauses().isEmpty()) {
                    completion.fail(errList.getCauses().get(0));
                    return;
                }
                logger.info(String.format("check and realize hardware vxlan networks[uuid: %s] for vm[uuid: %s] done", vxlanUuids, inv.getUuid()));
                completion.success();
            }
        });

        completion.await(TimeUnit.MINUTES.toMillis(30));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(operr("cannot configure hardware vxlan network for vm[uuid:%s] on the destination host[uuid:%s]",
                    inv.getUuid(), destHostUuid).causedBy(completion.getErrorCode()));
        }
    }

    @Override
    public void  beforeMigrateVm(VmInstanceInventory inv, String destHostUuid) {
    }

    @Override
    public void  afterMigrateVm(VmInstanceInventory inv, String srcHostUuid) {
    }

    @Override
    public void  failedToMigrateVm(VmInstanceInventory inv, String destHostUuid, ErrorCode reason) {
    }

    @Override
    public String getL2NetworkType() {
        return type.toString();
    }

    @Override
    public Integer getDefaultMtu(L2NetworkInventory inv) {
        return rcf.getResourceConfigValue(NetworkServiceGlobalConfig.DHCP_MTU_VLAN, inv.getUuid(), Integer.class);
    }

    @Override
    public Integer getL2NetworkVni(String l2NetworkUuid, String hostUuid) {
        VxlanNetworkVO vxlan = dbf.findByUuid(l2NetworkUuid, VxlanNetworkVO.class);
        HostInventory host = HostInventory.valueOf(dbf.findByUuid(hostUuid, HostVO.class));

        HardwareVxlanHelper.VxlanHostMappingStruct struct = HardwareVxlanHelper.getHardwareVxlanMappingVxlanId(
                L2VxlanNetworkInventory.valueOf(vxlan), host);

        return struct.getVlanId();
    }

    @Override
    public String getL2NetworkVniType() {
        return type.toString();
    }

    @Override
    public List<L2NetworkInventory> filterL2NetworkCascade(List<L2NetworkInventory> l2invs, CascadeAction action) {
        if (ZoneVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return l2invs.stream()
                    .filter(l2inv -> !l2inv.getType().equals(SdnControllerConstant.HARDWARE_VXLAN_NETWORK_TYPE))
                    .collect(Collectors.toList());
        } else if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<L2NetworkInventory> vxlans = l2invs.stream()
                    .filter(l2inv -> l2inv.getType().equals(SdnControllerConstant.HARDWARE_VXLAN_NETWORK_TYPE))
                    .collect(Collectors.toList());
            List<String> poolUuids = l2invs.stream()
                    .filter(l2inv -> l2inv.getType().equals(SdnControllerConstant.HARDWARE_VXLAN_NETWORK_POOL_TYPE))
                    .map(l2inv -> l2inv.getUuid())
                    .collect(Collectors.toList());
            if (vxlans.isEmpty()) {
                return l2invs;
            }
            vxlans.forEach(vxlan ->{
                VxlanNetworkVO vxlanNetworkVO = dbf.findByUuid(vxlan.getUuid(), VxlanNetworkVO.class);
                if(poolUuids.contains(vxlanNetworkVO.getPoolUuid())){
                    l2invs.remove(vxlan);
                }
            });
            return l2invs;
        } else {
            return l2invs;
        }
    }
}
