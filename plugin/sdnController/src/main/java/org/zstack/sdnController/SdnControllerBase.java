package org.zstack.sdnController;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l2.DeleteL2NetworkMsg;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.network.l2.SdnControllerDeleteExtensionPoint;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO_;
import org.zstack.sdnController.header.*;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.lang.reflect.Type;
import java.util.*;

import static java.util.Arrays.asList;
import static org.zstack.sdnController.header.SdnControllerFlowDataParam.SDN_CONTROLLER_UUID;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SdnControllerBase {
    private static final CLogger logger = Utils.getLogger(SdnControllerBase.class);
    @Autowired
    CloudBus bus;
    @Autowired
    DatabaseFacade dbf;
    @Autowired
    private CascadeFacade casf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private EventFacade evtf;
    @Autowired
    SdnControllerManager sdnMgr;
    @Autowired
    private PluginRegistry pluginRgty;

    public SdnControllerVO self;

    public SdnControllerBase(SdnControllerVO self) {
        this.self = self;
    }

    public String getSdnControllerSignature() {
        return "sdn-controller-" + self.getUuid();
    }

    protected SdnController getSdnController() {
        SdnControllerFactory factory = sdnMgr.getSdnControllerFactory(self.getVendorType());
        return factory.getSdnController(self);
    }

    protected SdnControllerL2 getSdnControllerL2() {
        SdnControllerFactory factory = sdnMgr.getSdnControllerFactory(self.getVendorType());
        return factory.getSdnControllerL2(self);
    }

    public void handleMessage(SdnControllerMessage msg) {
        if (msg instanceof APIRemoveSdnControllerMsg) {
            handle((APIRemoveSdnControllerMsg) msg);
        } else if (msg instanceof APIUpdateSdnControllerMsg) {
            handle((APIUpdateSdnControllerMsg) msg);
        } else if (msg instanceof SdnControllerDeletionMsg) {
            handle((SdnControllerDeletionMsg) msg);
        } else if (msg instanceof APISdnControllerAddHostMsg) {
            handle((APISdnControllerAddHostMsg) msg);
        } else if (msg instanceof APISdnControllerRemoveHostMsg) {
            handle((APISdnControllerRemoveHostMsg) msg);
        } else if (msg instanceof APIReconnectSdnControllerMsg) {
            handle((APIReconnectSdnControllerMsg) msg);
        } else if (msg instanceof APISdnControllerChangeHostMsg) {
            handle((APISdnControllerChangeHostMsg) msg);
        } else if (msg instanceof SdnControllerRemoveHostMsg) {
            handle((SdnControllerRemoveHostMsg) msg);
        } else if (msg instanceof ReconnectSdnControllerMsg) {
            handle((ReconnectSdnControllerMsg) msg);
        } else {
            SdnController controller = getSdnController();
            controller.handleMessage(msg);
        }
    }

    public void changeSdnControllerStatus(SdnControllerStatus status) {
        if (status == self.getStatus()) {
            return;
        }

        SdnControllerStatus oldStatus = self.getStatus();
        logger.debug(String.format("sdn controller [%s] changed status, old status: [%s], new status: [%s]",
                self.getUuid(), oldStatus, status.toString()));
        self.setStatus(status);
        self = dbf.updateAndRefresh(self);

        SdnControllerCanonicalEvents.SdnControllerStatusChangedData d = new SdnControllerCanonicalEvents.SdnControllerStatusChangedData();
        d.setSdnControllerUuid(self.getUuid());
        d.setSdnControllerType(self.getVendorType());
        d.setOldStatus(oldStatus.toString());
        d.setNewStatus(status.toString());
        d.setInv(SdnControllerInventory.valueOf(self));
        evtf.fire(SdnControllerCanonicalEvents.SDNCONTROLLER_STATUS_CHANGED_PATH, d);
    }

    private void handle(APIReconnectSdnControllerMsg msg) {
        APIReconnectSdnControllerEvent event = new APIReconnectSdnControllerEvent(msg.getId());
        reconnectSdnController(new Completion(msg) {
            @Override
            public void success() {
                event.setInventory(SdnControllerInventory.valueOf(dbf.findByUuid(msg.getSdnControllerUuid(), SdnControllerVO.class)));
                bus.publish(event);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                event.setError(errorCode);
                bus.publish(event);
            }
        });
    }

    private void handle(ReconnectSdnControllerMsg msg) {
        ReconnectSdnControllerReply reply = new ReconnectSdnControllerReply();
        reconnectSdnController(new Completion(msg) {
            @Override
            public void success() {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void reconnectSdnController(Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return getSdnControllerSignature();
            }

            @Override
            public void run(SyncTaskChain chain) {
                doReconnectSdnController(new Completion(completion) {
                    @Override
                    public void success() {
                        completion.success();
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("reconnect-sdn-controller-%s", self.getUuid());
            }
        });
    }

    private void doReconnectSdnController(Completion completion) {
        FlowChain chain = sdnMgr.getSyncChain(self);
        chain.getData().put(SDN_CONTROLLER_UUID, self.getUuid());
        chain.setName(String.format("sync-sdn-controller-%s-%s", self.getUuid(), self.getName()));
        chain.insert(new Flow() {
            String __name__ = "change-sdn-controller-status-to-connecting";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                changeSdnControllerStatus(SdnControllerStatus.Connecting);
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                changeSdnControllerStatus(SdnControllerStatus.Disconnected);
                trigger.rollback();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "change-sdn-controller-status-to-connected";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                changeSdnControllerStatus(SdnControllerStatus.Connected);
                trigger.next();
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

    private void sdnControllerAddHost(APISdnControllerAddHostMsg msg, Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return getSdnControllerSignature();
            }

            @Override
            public void run(SyncTaskChain chain) {
                SdnController controller = getSdnController();
                controller.addHost(msg, new Completion(completion) {
                    @Override
                    public void success() {
                        SdnControllerHostRefVO ref = new SdnControllerHostRefVO();
                        ref.setSdnControllerUuid(msg.getSdnControllerUuid());
                        ref.setHostUuid(msg.getHostUuid());
                        ref.setvSwitchType(msg.getvSwitchType());

                        Map<String, String> nicNameDriverMap = new HashMap<>();
                        Map<String, String> nicNamePciAddressMap = new HashMap<>();
                        List<Tuple> nicTuples = Q.New(HostNetworkInterfaceVO.class)
                                .eq(HostNetworkInterfaceVO_.hostUuid, msg.getHostUuid())
                                .in(HostNetworkInterfaceVO_.interfaceName, msg.getNicNames())
                                .select(HostNetworkInterfaceVO_.interfaceName,
                                        HostNetworkInterfaceVO_.driverType,
                                        HostNetworkInterfaceVO_.pciDeviceAddress)
                                .listTuple();
                        for (Tuple t : nicTuples) {
                            nicNameDriverMap.put(t.get(0, String.class), t.get(1, String.class));
                            nicNamePciAddressMap.put(t.get(0, String.class), t.get(2, String.class));
                        }
                        ref.setNicDrivers(JSONObjectUtil.toJsonString(nicNameDriverMap));
                        ref.setNicPciAddresses(JSONObjectUtil.toJsonString(nicNamePciAddressMap));
                        if (msg.getVtepIp() != null) {
                            ref.setVtepIp(msg.getVtepIp());
                        }
                        if (msg.getNetmask() != null) {
                            ref.setNetmask(msg.getNetmask());
                        }
                        if (msg.getBondMode() != null) {
                            ref.setBondMode(msg.getBondMode());
                        }
                        if (msg.getLacpMode() != null) {
                            ref.setLacpMode(msg.getLacpMode());
                        }
                        dbf.persist(ref);

                        completion.success();
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("sdn-controller-%s-add-host-%s",
                        self.getUuid(), msg.getHostUuid());
            }
        });
    }

    private void handle(APISdnControllerAddHostMsg msg) {
        APISdnControllerAddHostEvent event = new APISdnControllerAddHostEvent(msg.getId());
        sdnControllerAddHost(msg, new Completion(msg) {
            @Override
            public void success() {
                event.setInventory(SdnControllerInventory.valueOf(dbf.findByUuid(msg.getSdnControllerUuid(), SdnControllerVO.class)));
                bus.publish(event);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                event.setError(errorCode);
                bus.publish(event);
            }
        });
    }

    private void sdnControllerRemoveHost(SdnControllerRemoveHostMsg msg, Completion completion) {
        SdnController controller = getSdnController();
        controller.removeHost(msg, new Completion(completion) {
            @Override
            public void success() {
                SQL.New(SdnControllerHostRefVO.class)
                        .eq(SdnControllerHostRefVO_.sdnControllerUuid, msg.getSdnControllerUuid())
                        .eq(SdnControllerHostRefVO_.hostUuid, msg.getHostUuid())
                        .eq(SdnControllerHostRefVO_.vSwitchType, msg.getvSwitchType()).delete();
                self = dbf.reload(self);
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    private void sdnControllerRemoveHostInQueue(SdnControllerRemoveHostMsg msg, Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return getSdnControllerSignature();
            }

            @Override
            public void run(SyncTaskChain chain) {
                sdnControllerRemoveHost(msg, new Completion(completion) {
                    @Override
                    public void success() {
                        completion.success();
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("sdn-controller-%s-remove-host-%s",
                        self.getUuid(), msg.getHostUuid());
            }
        });
    }

    private void handle(APISdnControllerRemoveHostMsg amsg) {
        APISdnControllerRemoveHostEvent event = new APISdnControllerRemoveHostEvent(amsg.getId());

        SdnControllerRemoveHostMsg msg = SdnControllerRemoveHostMsg.fromApi(amsg);
        sdnControllerRemoveHostInQueue(msg, new Completion(msg) {
            @Override
            public void success() {
                event.setInventory(SdnControllerInventory.valueOf(dbf.findByUuid(msg.getSdnControllerUuid(), SdnControllerVO.class)));
                bus.publish(event);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                event.setError(errorCode);
                bus.publish(event);
            }
        });
    }

    private void handle(SdnControllerRemoveHostMsg msg) {
        SdnControllerRemoveHostReply reply = new SdnControllerRemoveHostReply();

        if (msg.isCreateChain()) {
            sdnControllerRemoveHostInQueue(msg, new Completion(msg) {
                @Override
                public void success() {
                    bus.reply(msg, reply);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                }
            });
        } else {
            sdnControllerRemoveHost(msg, new Completion(msg) {
                @Override
                public void success() {
                    bus.reply(msg, reply);
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    reply.setError(errorCode);
                    bus.reply(msg, reply);
                }
            });
        }
    }

    private void doControllerChangeHost(APISdnControllerChangeHostMsg msg, Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return getSdnControllerSignature();
            }

            @Override
            public void run(SyncTaskChain chain) {
                SdnControllerHostRefVO newRef = Q.New(SdnControllerHostRefVO.class)
                        .eq(SdnControllerHostRefVO_.sdnControllerUuid, msg.getSdnControllerUuid())
                        .eq(SdnControllerHostRefVO_.hostUuid, msg.getHostUuid()).find();
                SdnControllerHostRefVO oldRef = SdnControllerHostRefVO.fromOther(newRef);

                boolean changed = false;
                Gson gson = new Gson();
                Type type = new TypeToken<Map<String, String>>(){}.getType();
                Map<String, String> nicNamePciAddressMap = gson.fromJson(newRef.getNicPciAddresses(), type);
                List<String> oldNicNames = new ArrayList<>(nicNamePciAddressMap.keySet());
                Collections.sort(oldNicNames);
                Collections.sort(msg.getNicNames());
                if (!oldNicNames.equals(msg.getNicNames())) {
                    changed = true;
                    Map<String, String> nicNameDriverMap = new HashMap<>();
                    nicNamePciAddressMap = new HashMap<>();
                    List<Tuple> nicTuples = Q.New(HostNetworkInterfaceVO.class)
                            .eq(HostNetworkInterfaceVO_.hostUuid, msg.getHostUuid())
                            .in(HostNetworkInterfaceVO_.interfaceName, msg.getNicNames())
                            .select(HostNetworkInterfaceVO_.interfaceName,
                                    HostNetworkInterfaceVO_.driverType,
                                    HostNetworkInterfaceVO_.pciDeviceAddress)
                            .listTuple();
                    for (Tuple t : nicTuples) {
                        nicNameDriverMap.put(t.get(0, String.class), t.get(1, String.class));
                        nicNamePciAddressMap.put(t.get(0, String.class), t.get(2, String.class));
                    }
                    newRef.setNicDrivers(JSONObjectUtil.toJsonString(nicNameDriverMap));
                    newRef.setNicPciAddresses(JSONObjectUtil.toJsonString(nicNamePciAddressMap));
                }

                if (msg.getVtepIp() != null && !msg.getVtepIp().equals(newRef.getVtepIp())) {
                    changed = true;
                    newRef.setVtepIp(msg.getVtepIp());
                }

                if (msg.getNetmask() != null && !msg.getNetmask().equals(newRef.getNetmask())) {
                    changed = true;
                    newRef.setNetmask(msg.getNetmask());
                }

                if (msg.getBondMode() != null && !msg.getBondMode().equals(newRef.getBondMode())) {
                    changed = true;
                    newRef.setBondMode(msg.getBondMode());
                }

                if (msg.getLacpMode() != null && !msg.getLacpMode().equals(newRef.getLacpMode())) {
                    changed = true;
                    newRef.setLacpMode(msg.getLacpMode());
                }

                if (!changed) {
                    completion.success();
                    chain.next();
                    return;
                }

                SdnController controller = getSdnController();
                controller.changeHost(oldRef, newRef, new Completion(msg) {
                    @Override
                    public void success() {
                        dbf.update(newRef);
                        completion.success();
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("sdn-controller-%s-change-host-%s",
                        self.getUuid(), msg.getHostUuid());
            }
        });
    }

    private void handle(APISdnControllerChangeHostMsg msg) {
        APISdnControllerChangeHostEvent event = new APISdnControllerChangeHostEvent(msg.getId());
        doControllerChangeHost(msg, new Completion(msg) {
            @Override
            public void success() {
                self = dbf.reload(self);
                event.setInventory(SdnControllerInventory.valueOf(self));
                bus.publish(event);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                event.setError(errorCode);
                bus.publish(event);
            }
        });
    }

    private void doDeletionSdnController(SdnControllerDeletionMsg msg, Completion completion) {
        SdnController controller = getSdnController();

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("sdn-controller-deletion-%s", msg.getSdnControllerUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ =  String.format("detach-hardvxlan-network-of-sdn-controller-%s", self.getName());

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<HardwareL2VxlanNetworkPoolVO> poolVos = Q.New(HardwareL2VxlanNetworkPoolVO.class)
                        .eq(HardwareL2VxlanNetworkPoolVO_.sdnControllerUuid, msg.getSdnControllerUuid()).list();
                new While<>(poolVos).each((pool, wcomp) -> {
                    DeleteL2NetworkMsg msg = new DeleteL2NetworkMsg();
                    msg.setUuid(pool.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(msg, L2NetworkConstant.SERVICE_ID, pool.getUuid());
                    bus.send(msg, new CloudBusCallBack(wcomp) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.info(String.format("delete hardware vxpool[uuid:%s] failed, reason:%s", pool.getUuid(), reply.getError().getDetails()));
                            }
                            wcomp.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ =  "delete-sdn-network";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                SdnControllerL2 sdnControllerL2 = getSdnControllerL2();
                List<String> l2Uuids = sdnControllerL2.getL2NetworkOfSdnController();
                if (l2Uuids.isEmpty()) {
                    trigger.next();
                    return;
                }

                new While<>(l2Uuids).step((uuid, wcomp) -> {
                    DeleteL2NetworkMsg msg = new DeleteL2NetworkMsg();
                    msg.setUuid(uuid);
                    bus.makeTargetServiceIdByResourceUuid(msg, L2NetworkConstant.SERVICE_ID, uuid);
                    bus.send(msg, new CloudBusCallBack(wcomp) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.info(String.format("delete sdn l2 network[uuid:%s] failed, reason:%s", uuid, reply.getError().getDetails()));
                            }
                            wcomp.done();
                        }
                    });
                }, 5).run(new WhileDoneCompletion(trigger) {
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
            String __name__ =  "remove-host-from-sdn-controller";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<SdnControllerHostRefVO> refVOS = Q.New(SdnControllerHostRefVO.class)
                        .eq(SdnControllerHostRefVO_.sdnControllerUuid, msg.getSdnControllerUuid())
                        .list();
                if (refVOS.isEmpty()) {
                    trigger.next();
                    return;
                }

                new While<>(refVOS).step((ref, wcomp) -> {
                    SdnControllerRemoveHostMsg msg = new SdnControllerRemoveHostMsg();
                    msg.setSdnControllerUuid(ref.getSdnControllerUuid());
                    msg.setHostUuid(ref.getHostUuid());
                    msg.setvSwitchType(ref.getvSwitchType());
                    msg.setCreateChain(false);
                    bus.makeTargetServiceIdByResourceUuid(msg, SdnControllerConstant.SERVICE_ID, ref.getSdnControllerUuid());
                    bus.send(msg, new CloudBusCallBack(wcomp) {
                        @Override
                        public void run(MessageReply reply) {
                            if (!reply.isSuccess()) {
                                logger.debug(String.format("delete host [uuid:%s] from sdn controller[uuid:%s] failed, error:%s",
                                        msg.getHostUuid(), msg.getSdnControllerUuid(), reply.getError().getDetails()));
                            }
                            wcomp.done();
                        }
                    });
                }, 5).run(new WhileDoneCompletion(trigger) {
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
            String __name__ = String.format("delete-network-service");

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<SdnControllerDeleteExtensionPoint> exps = pluginRgty.getExtensionList(SdnControllerDeleteExtensionPoint.class);
                new While<>(exps).each((exp, wcomp) -> {
                    exp.deleteNetworkServiceOfSdnController(msg.getSdnControllerUuid(), new Completion(wcomp) {
                        @Override
                        public void success() {
                            wcomp.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.warn(String.format("delete network service failed, error: %s", errorCode.getDetails()));
                            wcomp.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = String.format("delete-from-sdn-controller");

            @Override
            public void run(FlowTrigger trigger, Map data) {
                controller.deleteSdnController(msg, SdnControllerInventory.valueOf(self), new Completion(completion) {
                    @Override
                    public void success() {
                        completion.success();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "delete-sdn-controller-on-db";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                dbf.removeByPrimaryKey(msg.getSdnControllerUuid(), SdnControllerVO.class);
                trigger.next();
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

    private void handle(SdnControllerDeletionMsg msg) {
        SdnControllerDeletionReply reply = new SdnControllerDeletionReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("sdn-controller-%s", msg.getSdnControllerUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                doDeletionSdnController(msg, new Completion(msg) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return String.format("delete-sdn-controller-%s", msg.getSdnControllerUuid());
            }
        });
    }

    private void handle(APIRemoveSdnControllerMsg msg) {
        APIRemoveSdnControllerEvent event = new APIRemoveSdnControllerEvent(msg.getId());

        final String issuer = SdnControllerVO.class.getSimpleName();
        SdnControllerVO vo = dbf.findByUuid(msg.getUuid(), SdnControllerVO.class);
        final List<SdnControllerInventory> ctx = asList(SdnControllerInventory.valueOf(vo));
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-sdn-controller-%s-name-%s", msg.getUuid(), vo.getName()));
        if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_CHECK_CODE, issuer, ctx, new Completion(trigger) {
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
            }).then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
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
        } else {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_FORCE_DELETE_CODE, issuer, ctx, new Completion(trigger) {
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
        }

        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                bus.publish(event);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                event.setError(errCode);
                bus.publish(event);
            }
        }).start();
    }

    private void handle(APIUpdateSdnControllerMsg msg) {
        APIUpdateSdnControllerEvent event = new APIUpdateSdnControllerEvent(msg.getId());
        SdnControllerVO vo = dbf.findByUuid(msg.getUuid(), SdnControllerVO.class);
        Boolean changed = false;

        if (msg.getName() != null && !msg.getName().equals(vo.getName())) {
            vo.setName(msg.getName());
            changed = true;
        }

        if (msg.getDescription() != null && !msg.getDescription().equals(vo.getDescription())) {
            vo.setDescription(msg.getDescription());
            changed = true;
        }

        if (changed) {
            vo = dbf.updateAndRefresh(vo);
        }

        event.setInventory(SdnControllerInventory.valueOf(vo));
        bus.publish(event);
    }
}
