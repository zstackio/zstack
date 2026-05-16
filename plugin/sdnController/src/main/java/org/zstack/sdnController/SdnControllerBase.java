package org.zstack.sdnController;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
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
import org.zstack.header.network.l3.SdnControllerL3;
import org.zstack.header.network.sdncontroller.*;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceVO_;
import org.zstack.sdnController.header.*;
import org.zstack.sdnController.h3cVcfc.H3cVcfcV2Commands;
import org.zstack.sdnController.h3cVcfc.H3cVcfcV2SdnController;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.sdnController.header.SdnControllerFlowDataParam.SDN_CONTROLLER_UUID;
import static org.zstack.sdnController.header.SdnControllerFlowDataParam.*;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

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
    SdnControllerManager sdnMgr;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private SdnControllerPingTracker sdnPingTracker;

    public SdnControllerVO self;

    public SdnControllerBase(SdnControllerVO self) {
        this.self = self;
    }

    public String getSdnControllerSignature() {
        return "sdn-controller-" + self.getUuid();
    }

    public String getSdnControllerHostSignature(String hostUuid) {
        return "sdn-controller-" + self.getUuid() + "-host-" + hostUuid;
    }

    protected SdnController getSdnController() {
        SdnControllerFactory factory = sdnMgr.getSdnControllerFactory(self.getVendorType());
        return factory.getSdnController(self);
    }

    protected SdnControllerL2 getSdnControllerL2() {
        SdnControllerFactory factory = sdnMgr.getSdnControllerFactory(self.getVendorType());
        return factory.getSdnControllerL2(self);
    }

    protected SdnControllerL3 getSdnControllerL3() {
        SdnControllerFactory factory = sdnMgr.getSdnControllerFactory(self.getVendorType());
        return factory.getSdnControllerL3(self);
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
        } else if (msg instanceof APIPullSdnControllerTenantMsg) {
            handle((APIPullSdnControllerTenantMsg) msg);
        } else if (msg instanceof APIChangeSdnControllerMsg) {
            handle((APIChangeSdnControllerMsg) msg);
        } else if (msg instanceof SdnControllerRemoveHostMsg) {
            handle((SdnControllerRemoveHostMsg) msg);
        } else if (msg instanceof PullSdnControllerTenantMsg) {
            handle((PullSdnControllerTenantMsg) msg);
        } else if (msg instanceof ReconnectSdnControllerMsg) {
            handle((ReconnectSdnControllerMsg) msg);
        } else if (msg instanceof SyncSdnControllerDataMsg) {
            handle((SyncSdnControllerDataMsg) msg);
        } else {
            SdnController controller = getSdnController();
            controller.handleMessage(msg);
        }
    }

    private void handle(SyncSdnControllerDataMsg msg) {
        SyncSdnControllerDataReply reply = new SyncSdnControllerDataReply();
        // Run a sync chain that only syncs data, without touching connection status
        thdf.chainSubmit(new ChainTask(reply) {
            @Override
            public String getSyncSignature() {
                return getSdnControllerSignature();
            }

            @Override
            public void run(SyncTaskChain chain) {
                FlowChain flowChain = sdnMgr.getSyncChain(self);
                flowChain.getData().put(SDN_CONTROLLER_UUID, self.getUuid());
                flowChain.setName(String.format("sync-sdn-controller-data-%s-%s", self.getUuid(), self.getName()));

                // allowEmptyFlow: vendors may not provide sync flows; treat empty chain as success
                flowChain.allowEmptyFlow();
                // Start the chain; flows in factory-provided chain should perform data sync operations
                flowChain.done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        bus.reply(msg, reply);
                        chain.next();
                    }
                }).error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        SyncSdnControllerDataReply r = new SyncSdnControllerDataReply();
                        r.setError(errCode);
                        bus.reply(msg, r);
                        chain.next();
                    }
                }).start();
            }

            @Override
            public String getName() {
                return String.format("sync-sdn-controller-data-%s", self.getUuid());
            }
        });
    }

    private void doChangeSdnController(APIChangeSdnControllerMsg msg, Completion completion) {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("change-sdn-controller-%s-%s", self.getUuid(), self.getName()));
        chain.then(new Flow() {
            String __name__ = "change-sdn-controller-db";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                boolean changed = false;

                // Handle name change
                if (msg.getUserName() != null && !msg.getUserName().equals(self.getName())) {
                    chain.getData().put(SDN_CONTROLLER_USERNAME, self.getName());
                    self.setUsername(msg.getUserName());
                    changed = true;
                }

                // Handle password change
                if (msg.getPassword() != null && !msg.getPassword().equals(self.getPassword())) {
                    chain.getData().put(SDN_CONTROLLER_PASSWORD, self.getPassword());
                    self.setPassword(msg.getPassword());
                    changed = true;
                }

                if (changed) {
                    String newUsername = self.getUsername();
                    String newPassword = self.getPassword();
                    self = dbf.reload(self);
                    self.setUsername(newUsername);
                    self.setPassword(newPassword);
                    self = dbf.updateAndRefresh(self);
                    chain.getData().put(SDN_CONTROLLER_CHANGED, changed);
                }

                if (msg.getVlanRanges() != null && !msg.getVlanRanges().isEmpty()) {
                    SdnControllerSystemTags.VLAN_RANGE.delete(self.getUuid());
                    for (String vlanRange : msg.getVlanRanges()) {
                        List<String> vlans = Arrays.asList(vlanRange.split("-"));
                        SystemTagCreator creator = SdnControllerSystemTags.VLAN_RANGE.newSystemTagCreator(self.getUuid());
                        creator.setTagByTokens(map(
                                e(SdnControllerSystemTags.START_VLAN_TOKEN, vlans.get(0)),
                                e(SdnControllerSystemTags.END_VLAN_TOKEN, vlans.get(1))));
                        creator.inherent = false;
                        creator.recreate = false;
                        creator.ignoreIfExisting = true;
                        creator.create();
                    }
                }

                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                String username = (String)chain.getData().get(SDN_CONTROLLER_USERNAME);
                String password = (String)chain.getData().get(SDN_CONTROLLER_PASSWORD);
                if (password != null) {
                    self.setPassword(password);
                }
                if (username != null) {
                    self.setUsername(username);
                }
                if (password != null || username != null) {
                    String rollbackUsername = self.getUsername();
                    String rollbackPassword = self.getPassword();
                    self = dbf.reload(self);
                    self.setUsername(rollbackUsername);
                    self.setPassword(rollbackPassword);
                    self = dbf.updateAndRefresh(self);
                }

                trigger.rollback();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "ping-sdn-controller";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                boolean changed = data.get(SDN_CONTROLLER_CHANGED) == null ? false : (boolean) data.get(SDN_CONTROLLER_CHANGED);
                if (!changed) {
                    // password not changed
                    trigger.next();
                    return;
                }

                SdnControllerPingMsg pmsg = new SdnControllerPingMsg();
                pmsg.setSdnControllerUuid(self.getUuid());
                bus.makeTargetServiceIdByResourceUuid(pmsg, SdnControllerConstant.SERVICE_ID, self.getUuid());
                bus.send(pmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            trigger.fail(operr(ORG_ZSTACK_SDNCONTROLLER_10009, "ping sdn controller failed, error: %s", reply.getError().getDetails()));
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

    private void changeSdnController(APIChangeSdnControllerMsg msg, Completion completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return getSdnControllerSignature();
            }

            @Override
            public void run(SyncTaskChain chain) {
                doChangeSdnController(msg, new Completion(completion) {
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
                return String.format("change-sdn-controller-%s", self.getUuid());
            }
        });
    }

    private void handle(APIChangeSdnControllerMsg msg) {
        APIChangeSdnControllerEvent event = new APIChangeSdnControllerEvent(msg.getId());
        changeSdnController(msg, new Completion(msg) {
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
                sdnMgr.getSdnControllerFactory(self.getVendorType()).changeSdnControllerStatus(self, SdnControllerStatusEvent.RECONNECT_STARTED);
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                sdnMgr.getSdnControllerFactory(self.getVendorType()).changeSdnControllerStatus(self, SdnControllerStatusEvent.RECONNECT_FAILED);
                trigger.rollback();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "reconnect-to-sdn-controller";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                SdnController controller = getSdnController();
                controller.reconnectSdnController(new Completion(trigger) {
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
            String __name__ = "change-sdn-controller-status-to-connected";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                sdnMgr.getSdnControllerFactory(self.getVendorType()).changeSdnControllerStatus(self, SdnControllerStatusEvent.RECONNECT_SUCCESS);
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
                return getSdnControllerHostSignature(msg.getHostUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                SdnControllerHostRefVO refvo = Q.New(SdnControllerHostRefVO.class)
                        .eq(SdnControllerHostRefVO_.sdnControllerUuid, msg.getSdnControllerUuid())
                        .eq(SdnControllerHostRefVO_.vSwitchType, msg.getvSwitchType())
                        .eq(SdnControllerHostRefVO_.vtepIp, msg.getVtepIp()).find();
                if (refvo != null) {
                    completion.fail(argerr(ORG_ZSTACK_SDNCONTROLLER_10010, "could not add host[uuid:%s] to sdn controller[uuid:%s], " +
                                    " because vtepip is used by host[uuid:%s]", msg.getHostUuid(),
                            msg.getSdnControllerUuid(), refvo.getHostUuid()));
                    return;
                }

                SdnController controller = getSdnController();
                controller.addHost(msg, new Completion(completion) {
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
                return getSdnControllerHostSignature(msg.getHostUuid());
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

        SdnControllerRemoveHostMsg msg = new SdnControllerRemoveHostMsg();
        msg.setSdnControllerUuid(amsg.getSdnControllerUuid());
        msg.setHostUuid(amsg.getHostUuid());
        msg.setvSwitchType(amsg.getvSwitchType());
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
                return getSdnControllerHostSignature(msg.getHostUuid());
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
                List<Tuple> l2Tuples = sdnControllerL2.getL2NetworkOfSdnController();
                List<String> l2Uuids = l2Tuples.stream().map(t -> t.get(0, String.class)).collect(Collectors.toList());
                if (l2Uuids.isEmpty()) {
                    trigger.next();
                    return;
                }

                new While<>(l2Uuids).each((uuid, wcomp) -> {
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
                }).run(new WhileDoneCompletion(trigger) {
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

                new While<>(refVOS).each((ref, wcomp) -> {
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
                }).run(new WhileDoneCompletion(trigger) {
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
                controller.deleteSdnController(msg, SdnControllerInventory.valueOf(self), new Completion(trigger) {
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
            String __name__ = "delete-sdn-controller-on-db";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                sdnPingTracker.untrack(msg.getSdnControllerUuid());
                controller.deleteSdnControllerDb(self);
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

    private void handle(APIPullSdnControllerTenantMsg amsg) {
        APIPullSdnControllerTenantEvent event = new APIPullSdnControllerTenantEvent(amsg.getId());

        PullSdnControllerTenantMsg msg = PullSdnControllerTenantMsg.fromApi(amsg);
        pullSdnControllerTenant(msg, new Completion(msg) {
            @Override
            public void success() {
                // After synchronization is complete, query and return the latest tenant data
                List<H3cSdnControllerTenantVO> tenantVOs = Q.New(H3cSdnControllerTenantVO.class)
                        .eq(H3cSdnControllerTenantVO_.sdnControllerUuid, msg.getSdnControllerUuid())
                        .list();
                List<H3cSdnControllerTenantInventory> inventories = H3cSdnControllerTenantInventory.valueOf(tenantVOs);
                event.setInventories(inventories);
                bus.publish(event);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                event.setError(errorCode);
                bus.publish(event);
            }
        });
    }

    private void handle(PullSdnControllerTenantMsg msg) {
        PullSdnControllerTenantReply reply = new PullSdnControllerTenantReply();

        pullSdnControllerTenant(msg, new Completion(msg) {
            @Override
            public void success() {
                // After synchronization is complete, query and return the latest tenant data
                List<H3cSdnControllerTenantVO> tenantVOs = Q.New(H3cSdnControllerTenantVO.class)
                        .eq(H3cSdnControllerTenantVO_.sdnControllerUuid, msg.getSdnControllerUuid())
                        .list();
                List<H3cSdnControllerTenantInventory> inventories = H3cSdnControllerTenantInventory.valueOf(tenantVOs);
                reply.setInventories(inventories);
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void pullSdnControllerTenant(PullSdnControllerTenantMsg msg, Completion completion) {
        // Only H3C VCFC V2 controllers support this operation, already validated in interceptor
        // But confirm again here to ensure type safety
        if (!SdnControllerConstant.H3C_VCFC_CONTROLLER.equals(self.getVendorType()) ||
            !SdnControllerConstant.H3C_VCFC_VENDOR_VERSION_V2.equals(self.getVendorVersion())) {
            completion.fail(operr(ORG_ZSTACK_SDNCONTROLLER_10011, "Pull tenant operation is only supported for H3C VCFC V2 controllers"));
            return;
        }

        // Get the correct controller instance through factory
        SdnControllerFactory factory = sdnMgr.getSdnControllerFactory(self.getVendorType());
        H3cVcfcV2SdnController h3cController = (H3cVcfcV2SdnController) factory.getSdnController(self);

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("pull-tenant-for-h3c-sdn-%s", self.getUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "pull-h3c-vds-tenant";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                try {
                    // Directly get all tenant information
                    h3cController.getH3cControllerToken(new Completion(trigger) {
                        @Override
                        public void success() {
                            List<H3cVcfcV2Commands.H3cTenantStruct> apiTenants = h3cController.getAllH3cTenants();
                            List<H3cVcfcV2Commands.H3cVdsStruct> apiVds = h3cController.getAllH3cVds();
                            syncTenantData(msg.getSdnControllerUuid(), apiTenants, apiVds, trigger);
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                } catch (Exception e) {
                    trigger.fail(operr(ORG_ZSTACK_SDNCONTROLLER_10012, "Failed to pull tenant data: %s", e.getMessage()));
                }
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "pull-h3c-vni-ranges";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                h3cController.getH3cVniRanges(new Completion(trigger) {
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

    private void syncTenantData(String sdnControllerUuid, List<H3cVcfcV2Commands.H3cTenantStruct> apiTenants, List<H3cVcfcV2Commands.H3cVdsStruct> apiVds, FlowTrigger trigger) {
        // Check if API response is valid (non-empty list indicates valid response with default tenant)
        if (apiTenants == null || apiTenants.isEmpty()) {
            logger.warn(String.format("Failed to pull tenant data for sdn controller [%s], no tenant data returned by API", sdnControllerUuid));
            trigger.next();
            return;
        }
        
        // Query existing tenant records in database
        List<H3cSdnControllerTenantVO> existingTenants = Q.New(H3cSdnControllerTenantVO.class)
                .eq(H3cSdnControllerTenantVO_.sdnControllerUuid, sdnControllerUuid)
                .list();

        // Create mapping table for easy lookup
        Map<String, H3cSdnControllerTenantVO> existingTenantMap = new HashMap<>();
        for (H3cSdnControllerTenantVO tenant : existingTenants) {
            String key = generateTenantKey(tenant.getTenantUuid(), tenant.getVdsUuid());
            existingTenantMap.put(key, tenant);
        }

        Set<String> apiTenantKeys = new HashSet<>();
        List<H3cSdnControllerTenantVO> tenantsToSave = new ArrayList<>();

        // Process tenant data returned by API
        for (H3cVcfcV2Commands.H3cTenantStruct apiTenant : apiTenants) {
            if (apiTenant.vds_list != null && !apiTenant.vds_list.isEmpty()) {
                String vdsUuid = apiTenant.vds_list.get(0);
                String key = generateTenantKey(apiTenant.id, vdsUuid);
                apiTenantKeys.add(key);

                H3cSdnControllerTenantVO existingTenant = existingTenantMap.get(key);
                if (existingTenant == null) {
                    // New tenant, create record
                    H3cSdnControllerTenantVO newTenant = new H3cSdnControllerTenantVO();
                    newTenant.setUuid(Platform.getUuid());
                    newTenant.setSdnControllerUuid(sdnControllerUuid);
                    newTenant.setTenantUuid(apiTenant.id);
                    newTenant.setVdsUuid(vdsUuid);
                    newTenant.setTenantName(apiTenant.name);
                    // Find vdsName from apiVds, use vdsUuid as fallback
                    String vdsName = vdsUuid;
                    for (H3cVcfcV2Commands.H3cVdsStruct vds : apiVds) {
                        if (Objects.equals(vds.uuid, vdsUuid) && vds.name != null) {
                            vdsName = vds.name;
                            break;
                        }
                    }
                    newTenant.setVdsName(vdsName);
                    newTenant.setCloudDomainName(apiTenant.cloud_domain_name);
                    newTenant.setState(SdnControllerConstant.H3C_SDN_CONTROLLER_TENANT_STATE_ENABLE);
                    tenantsToSave.add(newTenant);
                } else {
                    // Existing tenant, check if update is needed
                    boolean needUpdate = false;
                    if (!Objects.equals(existingTenant.getTenantName(), apiTenant.name)) {
                        existingTenant.setTenantName(apiTenant.name);
                        needUpdate = true;
                    }
                    if (!Objects.equals(existingTenant.getCloudDomainName(), apiTenant.cloud_domain_name)) {
                        existingTenant.setCloudDomainName(apiTenant.cloud_domain_name);
                        needUpdate = true;
                    }
                    // Ensure tenants found in API response are marked as enabled
                    if (!SdnControllerConstant.H3C_SDN_CONTROLLER_TENANT_STATE_ENABLE.equals(existingTenant.getState())) {
                        existingTenant.setState(SdnControllerConstant.H3C_SDN_CONTROLLER_TENANT_STATE_ENABLE);
                        needUpdate = true;
                    }
                    if (needUpdate) {
                        tenantsToSave.add(existingTenant);
                    }
                }
            }
        }

        // Handle tenants that exist in database but not in API (soft delete)
        for (H3cSdnControllerTenantVO existingTenant : existingTenants) {
            String key = generateTenantKey(existingTenant.getTenantUuid(), existingTenant.getVdsUuid());
            if (!apiTenantKeys.contains(key) && !SdnControllerConstant.H3C_SDN_CONTROLLER_TENANT_STATE_DISABLE.equals(existingTenant.getState())) {
                existingTenant.setState(SdnControllerConstant.H3C_SDN_CONTROLLER_TENANT_STATE_DISABLE);
                tenantsToSave.add(existingTenant);
            }
        }

        // Batch save updates
        if (!tenantsToSave.isEmpty()) {
            dbf.updateCollection(tenantsToSave);
        }

        trigger.next();
    }

    private String generateTenantKey(String tenantUuid, String vdsUuid) {
        return String.format("%s-%s", tenantUuid != null ? tenantUuid : "", vdsUuid != null ? vdsUuid : "");
    }
}
