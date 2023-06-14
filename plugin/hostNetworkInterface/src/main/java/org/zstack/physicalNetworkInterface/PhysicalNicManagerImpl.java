package org.zstack.physicalNetworkInterface;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.thread.RunInQueue;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.kvm.KVMHostConnectExtensionPoint;
import org.zstack.kvm.KVMHostConnectedContext;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.physicalNetworkInterface.header.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.Map;

public class PhysicalNicManagerImpl extends AbstractService implements PhysicalNicManager, KVMHostConnectExtensionPoint {
    private final static CLogger logger = Utils.getLogger(PhysicalNicManagerImpl.class);

    private final Map<String, HostVirtualNetworkInterfaceFactory> virtualNetworkInterfaceFactories = new HashMap<>();
    @Autowired
    private CloudBus bus;

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private ThreadFacade thdf;

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }

    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIConfigurePhysicalNicMsg) {
            handle((APIConfigurePhysicalNicMsg) msg);
        } else if (msg instanceof APIRecoverPhysicalNicMsg) {
            handle((APIRecoverPhysicalNicMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }

    }
    private void handleLocalMessage(Message msg) {
        if (msg instanceof ConfigurePhysicalNicMsg) {
            handle((ConfigurePhysicalNicMsg) msg);
        }
    }

    private void handle(APIConfigurePhysicalNicMsg msg) {
        APIConfigurePhysicalNicEvent evt = new APIConfigurePhysicalNicEvent(msg.getId());
        HostVirtualNetworkInterfaceFactory f = getVirtualNetworkInterfaceFactory(msg.getActionType());
        HostNetworkInterfaceVO interfaceVO= Q.New(HostNetworkInterfaceVO.class).eq(HostNetworkInterfaceVO_.uuid, msg.getPhysicalNicUuid()).find();
        HostNetworkInterfaceInventory hostNetworkInterface = HostNetworkInterfaceInventory.valueOf(interfaceVO);
        if (interfaceVO == null) {
            throw new OperationFailureException(Platform.operr("cloud not split host interface[uuid:%s], because it is deleted", msg.getPhysicalNicUuid()));
        }
        f.generateHostVirtualNetworkInterfaces(hostNetworkInterface, msg.getVirtPartNum(), false, new Completion(msg) {
            @Override
            public void success() {
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                evt.setError(errorCode);
                bus.publish(evt);
            }
        });
    }

    private void handle(APIRecoverPhysicalNicMsg msg) {
        APIRecoverPhysicalNicEvent evt = new APIRecoverPhysicalNicEvent(msg.getId());
        HostVirtualNetworkInterfaceFactory f = getVirtualNetworkInterfaceFactory(msg.getActionType());
        HostNetworkInterfaceVO interfaceVO= Q.New(HostNetworkInterfaceVO.class).eq(HostNetworkInterfaceVO_.uuid, msg.getPhysicalNicUuid()).find();
        if (interfaceVO == null) {
            throw new OperationFailureException(Platform.operr("cloud not split host interface[uuid:%s], because it is deleted", msg.getPhysicalNicUuid()));
        }
        f.ungenerateHostVirtualNetworkInterface(HostNetworkInterfaceInventory.valueOf(interfaceVO), new Completion(msg) {
            @Override
            public void success() {
                bus.publish(evt);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.debug(String.format("failed to recover physical nic, because %s", errorCode));
                new SQLBatch(){
                    @Override
                    protected void scripts() {
                        SQL.New(HostVirtualNetworkInterfaceVO.class).eq(HostVirtualNetworkInterfaceVO_.hostNetworkInterfaceUuid, msg.getPhysicalNicUuid()).delete();
                        SQL.New(HostNetworkInterfaceVO.class).eq(HostNetworkInterfaceVO_.uuid, msg.getPhysicalNicUuid()).set(HostNetworkInterfaceVO_.virtStatus, NicVirtStatus.VIRTUALIZABLE).update();
                    }
                }.execute();
                bus.publish(evt);
            }
        });
    }
    private void handle(ConfigurePhysicalNicMsg msg) {
        ConfigurePhysicalNicReply reply = new ConfigurePhysicalNicReply();
        HostVirtualNetworkInterfaceFactory f = getVirtualNetworkInterfaceFactory(msg.getActionType().toString());
        HostNetworkInterfaceVO interfaceVO= Q.New(HostNetworkInterfaceVO.class).eq(HostNetworkInterfaceVO_.uuid, msg.getPhysicalNicUuid()).find();
        if (interfaceVO == null) {
            throw new OperationFailureException(Platform.operr("cloud not split host interface[uuid:%s], because it is deleted", msg.getPhysicalNicUuid()));
        }
        SplitHostNetworkInterfaceStruct struct = new SplitHostNetworkInterfaceStruct();
        struct.setActionType(PhysicalNicActionType.valueOf(msg.getActionType().toString()));
        struct.setHostNetworkInterface(HostNetworkInterfaceInventory.valueOf(interfaceVO));
        struct.setReConfigure(false);
        struct.setVirtPartNum(msg.getVirtPartNum());
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(PhysicalNicConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        populateExtensions();

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void populateExtensions() {
        pluginRgty.getExtensionList(HostVirtualNetworkInterfaceFactory.class).forEach(f -> {
            HostVirtualNetworkInterfaceFactory old = virtualNetworkInterfaceFactories.get(f.getActionType());
            if (old != null) {
                throw new CloudRuntimeException(String.format(
                        "duplicate VirtualHostNetworkInterfaceFactory[%s, %s] with the same type[%s]",
                        old.getClass(), f.getClass(), f.getActionType()
                ));
            }

            virtualNetworkInterfaceFactories.put(f.getActionType(), f);
        });
    }

    private HostVirtualNetworkInterfaceFactory getVirtualNetworkInterfaceFactory(String actionType) {
        HostVirtualNetworkInterfaceFactory f = virtualNetworkInterfaceFactories.get(actionType);
        if (f == null) {
            throw new CloudRuntimeException("cannot find VirtualPciDeviceFactory with type " + actionType);
        }
        return f;
    }

    protected RunInQueue inQueue(String queueId, int syncLevel) {
        return new RunInQueue(queueId, thdf, syncLevel);
    }

    private void syncPhysicalNicsWhenConnectHost(KVMHostConnectedContext context, Completion completion) {
        KVMHostInventory host = context.getInventory();

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("sync-physical-nics-for-host-%s", host.getUuid()));

        chain.then(new NoRollbackFlow() {
            String __name__= "get-host-network-facts";
            @Override
            public void run(FlowTrigger trigger1, Map data) {
                GetHostNetworkFactsMsg gmsg = new GetHostNetworkFactsMsg();
                gmsg.setHostUuid(host.getUuid());
                gmsg.setNoStatusCheck(true);
                bus.makeTargetServiceIdByResourceUuid(gmsg, HostConstant.SERVICE_ID, host.getUuid());
                bus.send(gmsg, new CloudBusCallBack(trigger1) {
                    @Override
                    public void run(MessageReply reply) {
                        ErrorCode error = null;
                        if (!reply.isSuccess()) {
                            error = reply.getError();
                        } else {
                            GetHostNetworkFactsReply rsp = reply.castReply();
                            if (!rsp.isSuccess()) {
                                error = rsp.getError();
                            }
                        }

                        if (error != null) {
                            logger.warn(String.format("failed to get network facts after host[uuid:%s] connected", host.getUuid()));
                        }
                        // trigger next anyway
                        trigger1.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__= "get-host-physical-memory-facts";
            @Override
            public void run(FlowTrigger trigger1, Map data) {
                GetHostPhysicalMemoryFactsMsg gmsg = new GetHostPhysicalMemoryFactsMsg();
                gmsg.setHostUuid(host.getUuid());
                gmsg.setNoStatusCheck(true);
                bus.makeTargetServiceIdByResourceUuid(gmsg, HostConstant.SERVICE_ID, host.getUuid());
                bus.send(gmsg, new CloudBusCallBack(trigger1) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!reply.isSuccess()) {
                            logger.warn(String.format("failed to get physical memory facts after host[uuid:%s] connected, error: %s", host.getUuid(), reply.getError().getDetails()));
                        }

                        // trigger next anyway
                        trigger1.next();
                    }
                });
            }
        });

        for (SyncPhysicalNicExtensionPoint ext : pluginRgty.getExtensionList(SyncPhysicalNicExtensionPoint.class)) {
            Flow flow = ext.createPhysicalNicsPostSyncFlow(host);
            if (flow != null) {
                chain.then(flow);
            }
        }
        chain.done(new FlowDoneHandler(completion) {
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
    public Flow createKvmHostConnectingFlow(KVMHostConnectedContext context) {
        return new NoRollbackFlow() {
            @Override
            public void run(FlowTrigger trigger, Map data) {
                syncPhysicalNicsWhenConnectHost(context, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format("failed to sync network facts after host[uuid:%s] connected, %s", context.getInventory().getUuid(), errorCode));
                        trigger.next();
                    }
                });
            }
        };
    }
}
