package org.zstack.sdnController.hardwareVxlan;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.*;
import org.zstack.network.l2.vxlan.vtep.*;
import org.zstack.network.l2.vxlan.vxlanNetwork.L2VxlanNetworkInventory;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO_;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.*;
import org.zstack.sdnController.SdnControllerFactory;
import org.zstack.sdnController.SdnControllerL2;
import org.zstack.sdnController.SdnControllerManager;
import org.zstack.sdnController.header.HardwareL2VxlanNetworkPoolInventory;
import org.zstack.sdnController.header.HardwareL2VxlanNetworkPoolVO;
import org.zstack.header.network.sdncontroller.SdnControllerVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static org.zstack.core.Platform.operr;

/**
 * Created by shixin.ruan on 09/17/2019.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HardwareVxlanNetworkPool extends VxlanNetworkPool {
    @Autowired
    SdnControllerManager sdnControllerManager;

    private static final CLogger logger = Utils.getLogger(HardwareVxlanNetworkPool.class);

    public HardwareVxlanNetworkPool(L2NetworkVO vo) {
        super(vo);
    }

    private HardwareL2VxlanNetworkPoolVO getSelf1() {
        return (HardwareL2VxlanNetworkPoolVO) self;
    }

    @Override
    protected HardwareL2VxlanNetworkPoolInventory getSelfInventory() {
        return HardwareL2VxlanNetworkPoolInventory.valueOf(getSelf1());
    }

    @Override
    protected void handle(final PopulateVtepPeersMsg msg) {
        throw new CloudRuntimeException("HardwareVxlanNetworkPool don't need vtep");
    }

    @Override
    protected void handle(CreateVtepMsg msg) {
        throw new CloudRuntimeException("HardwareVxlanNetworkPool don't need vtep");
    }

    @Override
    protected void handle(DeleteVtepMsg msg) {
        throw new CloudRuntimeException("HardwareVxlanNetworkPool don't need vtep");
    }

    @Override
    protected void handle(final APICreateVxlanVtepMsg msg) {
        throw new CloudRuntimeException("HardwareVxlanNetworkPool don't need vtep");
    }

    @Override
    protected void afterDetachVxlanPoolFromCluster(APIDetachL2NetworkFromClusterMsg msg) {
        super.afterDetachVxlanPoolFromCluster(msg);
    }

    @Override
    public void prepareL2NetworkOnHosts(final String l2NetworkUuid, final List<HostInventory> hosts, boolean applyToSdn, final Completion completion) {
        List<VxlanNetworkVO> vxlanVos = Q.New(VxlanNetworkVO.class)
                .eq(VxlanNetworkVO_.poolUuid, self.getUuid()).list();
        SdnControllerVO vo = dbf.findByUuid(getSelf1().getSdnControllerUuid(), SdnControllerVO.class);
        SdnControllerFactory factory = sdnControllerManager.getSdnControllerFactory(vo.getVendorType());
        if (factory == null) {
            completion.fail(operr("there is no sdn controller factory for sdn controller type:%s", vo.getVendorType()));
            return;
        }

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("prepare-l2-%s-on-hosts", self.getUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "check-physical-interface";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                List<CheckNetworkPhysicalInterfaceMsg> cmsgs = new ArrayList<CheckNetworkPhysicalInterfaceMsg>();
                for (HostInventory h : hosts) {
                    CheckNetworkPhysicalInterfaceMsg cmsg = new CheckNetworkPhysicalInterfaceMsg();
                    cmsg.setHostUuid(h.getUuid());
                    cmsg.setPhysicalInterface(self.getPhysicalInterface());
                    bus.makeTargetServiceIdByResourceUuid(cmsg, HostConstant.SERVICE_ID, h.getUuid());
                    cmsgs.add(cmsg);
                }

                if (cmsgs.isEmpty()) {
                    trigger.next();
                    return;
                }

                new While<>(cmsgs).step((msg, wcomp) -> {
                    bus.send(msg, new CloudBusCallBack(wcomp) {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                wcomp.done();
                            } else {
                                wcomp.addError(reply.getError());
                                wcomp.allDone();
                            }
                        }
                    });
                }, L2NetworkConstant.MAX_PARALLEL_HOST_MSG).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (errorCodeList.getCauses().isEmpty()) {
                            trigger.next();
                        } else {
                            trigger.fail(errorCodeList.getCauses().get(0));
                        }
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "realize-vxlan-network";

            @Override
            public void run(final FlowTrigger trigger, Map data) {
                if (vxlanVos.isEmpty()) {
                    trigger.next();
                    return;
                }

                new While<>(vxlanVos).each((vxlan, whileCompletion) -> {
                    HardwareVxlanNetwork nw = new HardwareVxlanNetwork(vxlan);
                    nw.attachL2NetworkToHosts(L2VxlanNetworkInventory.valueOf(vxlan),
                            hosts, new Completion(whileCompletion) {
                        @Override
                        public void success() {
                            whileCompletion.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            whileCompletion.addError(errorCode);
                            whileCompletion.allDone();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errorCodeList.getCauses().isEmpty()) {
                            trigger.fail(errorCodeList.getCauses().get(0));
                        } else {
                            trigger.next();
                        }
                    }

                });
            }

        }).then(new NoRollbackFlow() {
            String __name__ = "attach-vxlan-network-on-sdn";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (vxlanVos.isEmpty()) {
                    trigger.next();
                    return;
                }

                if (!applyToSdn) {
                    trigger.next();
                    return;
                }

                SdnControllerL2 controller = factory.getSdnControllerL2(vo);
                new While<>(vxlanVos).each((vxlan, whileCompletion) -> {
                    controller.attachL2NetworkToHosts(L2VxlanNetworkInventory.valueOf(vxlan), hosts, new ArrayList<>(), new Completion(whileCompletion) {
                        @Override
                        public void success() {
                            whileCompletion.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            whileCompletion.addError(errorCode);
                            whileCompletion.allDone();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errorCodeList.getCauses().isEmpty()) {
                            trigger.fail(errorCodeList.getCauses().get(0));
                        } else {
                            trigger.next();
                        }
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

    @Override
    public void deleteHook(Completion completion) {
        /* delete all l2 network of this pool */
        List<String> vxlanUuids = Q.New(VxlanNetworkVO.class).eq(VxlanNetworkVO_.poolUuid, self.getUuid())
                .select(VxlanNetworkVO_.uuid).listValues();

        new While<>(vxlanUuids).all((uuid, wcomp) -> {
            DeleteL2NetworkMsg msg = new DeleteL2NetworkMsg();
            msg.setUuid(uuid);
            bus.makeTargetServiceIdByResourceUuid(msg, L2NetworkConstant.SERVICE_ID, uuid);
            bus.send(msg, new CloudBusCallBack(wcomp) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        logger.info(String.format("delete hardware vxlan network[uuid:%s] failed, reason:%s", uuid, reply.getError().getDetails()));
                    }
                    wcomp.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }
}
