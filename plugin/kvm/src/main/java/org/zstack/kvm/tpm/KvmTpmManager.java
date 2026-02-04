package org.zstack.kvm.tpm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.devices.VmTpmManager;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.tpm.api.APIAddTpmEvent;
import org.zstack.header.tpm.api.APIAddTpmMsg;
import org.zstack.header.tpm.api.APIGetTpmCapabilityMsg;
import org.zstack.header.tpm.api.APIGetTpmCapabilityReply;
import org.zstack.header.tpm.api.APIRemoveTpmEvent;
import org.zstack.header.tpm.api.APIRemoveTpmMsg;
import org.zstack.header.tpm.api.APIUpdateTpmMsg;
import org.zstack.header.tpm.entity.TpmCapabilityView;
import org.zstack.header.tpm.entity.TpmInventory;
import org.zstack.header.tpm.entity.TpmVO;
import org.zstack.header.tpm.entity.TpmVO_;
import org.zstack.header.tpm.message.AddTpmMsg;
import org.zstack.header.tpm.message.AddTpmReply;
import org.zstack.header.tpm.message.RemoveTpmMsg;
import org.zstack.header.tpm.message.RemoveTpmReply;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

import static org.zstack.compute.vm.VmGlobalConfig.RESET_TPM_AFTER_VM_CLONE;
import static org.zstack.core.Platform.err;
import static org.zstack.header.errorcode.SysErrors.NOT_SUPPORTED;
import static org.zstack.header.tpm.TpmConstants.*;
import static org.zstack.header.tpm.TpmErrors.VM_STATE_ERROR;
import static org.zstack.kvm.KVMSystemTags.EDK_RPM_TOKEN;
import static org.zstack.kvm.KVMSystemTags.SWTPM_VERSION;
import static org.zstack.kvm.KVMSystemTags.SWTPM_VERSION_TOKEN;
import static org.zstack.kvm.KVMSystemTags.VM_EDK;

public class KvmTpmManager extends AbstractService {
    private static final CLogger logger = Utils.getLogger(KvmTpmManager.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade threadFacade;
    @Autowired
    private ResourceConfigFacade resourceConfigFacade;
    @Autowired
    private VmTpmManager vmTpmManager;

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }

    private String tpmQueueSyncSignature(String vmUuid) {
        return String.format("tpm-queue-sync-%s", vmUuid);
    }

    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof AddTpmMsg) {
            handle((AddTpmMsg) msg);
        } else if (msg instanceof RemoveTpmMsg) {
            handle((RemoveTpmMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIGetTpmCapabilityMsg) {
            handle((APIGetTpmCapabilityMsg) msg);
        } else if (msg instanceof APIAddTpmMsg) {
            handle((APIAddTpmMsg) msg);
        } else if (msg instanceof APIRemoveTpmMsg) {
            handle((APIRemoveTpmMsg) msg);
        } else if (msg instanceof APIUpdateTpmMsg) {
            handle((APIUpdateTpmMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(AddTpmMsg msg) {
        AddTpmReply reply = new AddTpmReply();
        threadFacade.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                AddTpmToVmContext context = AddTpmToVmContext.valueOf(msg);
                addTpmToVm(context, new Completion(chain, msg) {
                    @Override
                    public void success() {
                        chain.next();
                        TpmVO vo = Q.New(TpmVO.class)
                                .eq(TpmVO_.uuid, msg.getTpmUuid())
                                .find();
                        reply.setInventory(TpmInventory.valueOf(vo));
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        chain.next();
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return tpmQueueSyncSignature(msg.getVmInstanceUuid());
            }

            @Override
            public String getName() {
                return "queue-of-add-tpm-to-vm-" + msg.getVmInstanceUuid();
            }
        });
    }

    static class AddTpmToVmContext {
        String keyProviderUuid;
        String vmInstanceUuid;
        String tpmUuid;

        static AddTpmToVmContext valueOf(AddTpmMsg msg) {
            AddTpmToVmContext context = new AddTpmToVmContext();
            context.keyProviderUuid = msg.getKeyProviderUuid();
            context.vmInstanceUuid = msg.getVmInstanceUuid();
            context.tpmUuid = msg.getTpmUuid();
            return context;
        }
    }

    @SuppressWarnings("rawtypes")
    private void addTpmToVm(AddTpmToVmContext context, Completion completion) {
        SimpleFlowChain chain = new SimpleFlowChain();
        chain.setName("add-tpm-to-vm-" + context.vmInstanceUuid);
        chain.then(new NoRollbackFlow() {
            String __name__ = "check-vm-status";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                VmInstanceVO vm = Q.New(VmInstanceVO.class)
                        .eq(VmInstanceVO_.uuid, context.vmInstanceUuid)
                        .find();

                if (!SUPPORT_VM_STATES_FOR_TPM_OPERATION.contains(vm.getState())) {
                    trigger.fail(err(VM_STATE_ERROR,
                            "The current VM state does not support adding TPM operations")
                            .withOpaque("support.vm.state", SUPPORT_VM_STATES_FOR_TPM_OPERATION));
                    return;
                }
                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "create-tpm-db-records";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                vmTpmManager.persistTpmVO(context.tpmUuid, context.vmInstanceUuid);
                trigger.next();
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errorCode, Map data) {
                completion.fail(errorCode);
            }
        }).start();
    }

    private void handle(RemoveTpmMsg msg) {
        RemoveTpmReply reply = new RemoveTpmReply();
        threadFacade.chainSubmit(new ChainTask(msg) {
            @Override
            public void run(SyncTaskChain chain) {
                RemoveTpmFromVmContext context = RemoveTpmFromVmContext.valueOf(msg);
                removeTpmFromVm(context, new Completion(chain, msg) {
                    @Override
                    public void success() {
                        chain.next();
                        bus.reply(msg, reply);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        chain.next();
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                    }
                });
            }

            @Override
            public String getSyncSignature() {
                return tpmQueueSyncSignature(msg.getVmInstanceUuid());
            }

            @Override
            public String getName() {
                return "queue-of-remove-tpm-from-vm-" + msg.getVmInstanceUuid();
            }
        });
    }

    static class RemoveTpmFromVmContext {
        String vmInstanceUuid;
        String tpmUuid;

        static RemoveTpmFromVmContext valueOf(RemoveTpmMsg msg) {
            RemoveTpmFromVmContext context = new RemoveTpmFromVmContext();
            context.vmInstanceUuid = msg.getVmInstanceUuid();
            context.tpmUuid = msg.getTpmUuid();
            return context;
        }
    }

    @SuppressWarnings("rawtypes")
    private void removeTpmFromVm(RemoveTpmFromVmContext context, Completion completion) {
        SimpleFlowChain chain = new SimpleFlowChain();
        chain.setName("remove-tpm-from-vm-" + context.vmInstanceUuid);
        chain.then(new NoRollbackFlow() {
            String __name__ = "check-vm-status";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                VmInstanceVO vm = Q.New(VmInstanceVO.class)
                        .eq(VmInstanceVO_.uuid, context.vmInstanceUuid)
                        .find();

                if (!SUPPORT_VM_STATES_FOR_TPM_OPERATION.contains(vm.getState())) {
                    trigger.fail(err(VM_STATE_ERROR,
                            "The current VM state does not support removing TPM operations")
                            .withOpaque("support.vm.state", SUPPORT_VM_STATES_FOR_TPM_OPERATION));
                    return;
                }
                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "remove-tpm-db-records";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                SQL.New(TpmVO.class)
                        .eq(TpmVO_.uuid, context.tpmUuid)
                        .delete();
                trigger.next();
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errorCode, Map data) {
                completion.fail(errorCode);
            }
        }).start();
    }

    private void handle(APIGetTpmCapabilityMsg msg) {
        TpmCapabilityView view = new TpmCapabilityView();

        final TpmVO tpm = Q.New(TpmVO.class)
                .eq(TpmVO_.uuid, msg.getTpmUuid())
                .find();
        final VmInstanceVO vm = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, tpm.getVmInstanceUuid())
                .find();

        view.setTpmInventory(TpmInventory.valueOf(tpm));
        view.setEdkVersion(VM_EDK.getTokenByResourceUuid(vm.getUuid(), EDK_RPM_TOKEN));

        if (vm.getHostUuid() != null) {
            view.setSwtpmVersion(SWTPM_VERSION.getTokenByResourceUuid(vm.getHostUuid(), SWTPM_VERSION_TOKEN));
        } else if (vm.getLastHostUuid() != null) {
            view.setSwtpmVersion(SWTPM_VERSION.getTokenByResourceUuid(vm.getLastHostUuid(), SWTPM_VERSION_TOKEN));
        }

        ResourceConfig resourceConfig = resourceConfigFacade.getResourceConfig(RESET_TPM_AFTER_VM_CLONE.getIdentity());
        view.setResetTpmAfterVmCloneConfig(resourceConfig.getResourceConfigValue(vm.getUuid(), Boolean.class));

        APIGetTpmCapabilityReply reply = new APIGetTpmCapabilityReply();
        reply.setInventory(view);
        bus.reply(msg, reply);
    }

    private void handle(APIAddTpmMsg msg) {
        APIAddTpmEvent event = new APIAddTpmEvent(msg.getId());

        AddTpmMsg inner = AddTpmMsg.valueOf(msg);
        bus.makeTargetServiceIdByResourceUuid(inner, SERVICE_ID, msg.getResourceUuid());
        bus.send(inner, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    event.setInventory(((AddTpmReply) reply.castReply()).getInventory());
                } else {
                    event.setError(reply.getError());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(APIRemoveTpmMsg msg) {
        APIRemoveTpmEvent event = new APIRemoveTpmEvent(msg.getId());

        RemoveTpmMsg inner = RemoveTpmMsg.valueOf(msg);
        bus.makeTargetServiceIdByResourceUuid(inner, SERVICE_ID, msg.getTpmUuid());
        bus.send(inner, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    event.setError(reply.getError());
                }
                bus.publish(event);
            }
        });
    }

    private void handle(APIUpdateTpmMsg msg) {
        throw err(NOT_SUPPORTED, "UpdateTpm is not supported in current version").toException();
    }
}
