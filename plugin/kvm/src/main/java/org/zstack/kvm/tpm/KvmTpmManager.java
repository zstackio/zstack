package org.zstack.kvm.tpm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.devices.TpmEncryptedResourceKeyBackend;
import org.zstack.compute.vm.devices.VmTpmManager;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
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
import org.zstack.header.vm.additions.ResetVmTpmMsg;
import org.zstack.header.vm.additions.ResetVmTpmReply;
import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.vm.additions.VmHostBackupFileVO_;
import org.zstack.header.vm.additions.VmHostFileInventory;
import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.header.vm.additions.VmHostFileVO;
import org.zstack.header.vm.additions.VmHostFileVO_;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.zstack.compute.vm.VmGlobalConfig.RESET_TPM_AFTER_VM_CLONE;
import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.header.errorcode.SysErrors.NOT_SUPPORTED;
import static org.zstack.header.tpm.TpmConstants.*;
import static org.zstack.header.tpm.TpmErrors.VM_STATE_ERROR;
import static org.zstack.kvm.KVMSystemTags.EDK_RPM_TOKEN;
import static org.zstack.kvm.KVMSystemTags.SWTPM_VERSION;
import static org.zstack.kvm.KVMSystemTags.SWTPM_VERSION_TOKEN;
import static org.zstack.kvm.KVMSystemTags.VM_EDK;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.transform;

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
    @Autowired
    private TpmEncryptedResourceKeyBackend tpmKeyBackend;

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
        } else if (msg instanceof CloneVmTpmMsg) {
            handle((CloneVmTpmMsg) msg);
        } else if (msg instanceof ResetVmTpmMsg) {
            handle((ResetVmTpmMsg) msg);
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

        boolean tpmCreated;
        boolean keyProviderAttached;
        String createdTpmUuid;

        static AddTpmToVmContext valueOf(AddTpmMsg msg) {
            AddTpmToVmContext context = new AddTpmToVmContext();
            context.keyProviderUuid = msg.getKeyProviderUuid();
            context.vmInstanceUuid = msg.getVmInstanceUuid();
            context.tpmUuid = msg.getTpmUuid();
            return context;
        }
    }

    private void addTpmToVm(AddTpmToVmContext context, Completion completion) {
        SimpleFlowChain.of("add-tpm-to-vm-" + context.vmInstanceUuid)
            .then(Flow.of("check-vm-status")
                .handle(trigger -> {
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
                })
                .build())
            .then(Flow.of("create-tpm-db-records")
                .handle(trigger -> {
                    try {
                        TpmVO tpm = vmTpmManager.persistTpmVO(context.tpmUuid, context.vmInstanceUuid);
                        context.createdTpmUuid = tpm.getUuid();
                        context.tpmCreated = true;
                        if (context.keyProviderUuid != null) {
                            tpmKeyBackend.attachKeyProviderToTpm(context.createdTpmUuid, context.keyProviderUuid);
                            context.keyProviderAttached = true;
                        }
                        trigger.next();
                    } catch (Exception e) {
                        trigger.fail(operr("failed to add TPM to vm[uuid:%s]: %s", context.vmInstanceUuid, e.getMessage()));
                    }
                })
                .rollback(trigger -> {
                    try {
                        if (context.keyProviderAttached && context.createdTpmUuid != null) {
                            tpmKeyBackend.detachKeyProviderFromTpm(context.createdTpmUuid);
                        }
                    } finally {
                        if (context.tpmCreated && context.createdTpmUuid != null) {
                            vmTpmManager.deleteTpmVO(context.createdTpmUuid);
                        }
                    }
                    trigger.rollback();
                })
                .build())
            .propagateExceptionTo(completion)
            .done(completion::success)
            .error(completion::fail)
            .start();
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

    private void removeTpmFromVm(RemoveTpmFromVmContext context, Completion completion) {
        SimpleFlowChain.of("remove-tpm-from-vm-" + context.vmInstanceUuid)
            .then(Flow.of("check-vm-status")
                .handle(trigger -> {
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
                })
                .build())
            .then(Flow.of("detach-resource-key")
                .handle(trigger -> {
                    tpmKeyBackend.detachKeyProviderFromTpm(context.tpmUuid);
                    trigger.next();
                })
                .build())
            .then(Flow.of("remove-tpm-db-records")
                .handle(trigger -> {
                    new SQLBatch() {
                        @Override
                        protected void scripts() {
                            Set<VmHostFileType> types = new HashSet<>();
                            types.add(VmHostFileType.TpmState);

                            sql(TpmVO.class)
                                    .eq(TpmVO_.uuid, context.tpmUuid)
                                    .delete();

                            boolean needRegisterNvRam = vmTpmManager.needRegisterNvRam(context.vmInstanceUuid);
                            if (!needRegisterNvRam) {
                                types.add(VmHostFileType.NvRam);
                            }

                            sql(VmHostFileVO.class)
                                    .eq(VmHostFileVO_.vmInstanceUuid, context.vmInstanceUuid)
                                    .in(VmHostFileVO_.type, types)
                                    .delete();
                            sql(VmHostBackupFileVO.class)
                                    .eq(VmHostBackupFileVO_.resourceUuid, context.vmInstanceUuid)
                                    .in(VmHostBackupFileVO_.type, types)
                                    .delete();
                        }
                    }.execute();
                    trigger.next();
                })
                .build())
            .propagateExceptionTo(completion)
            .done(completion::success)
            .error(completion::fail)
            .start();
    }

    @SuppressWarnings("rawtypes")
    private void handle(CloneVmTpmMsg msg) {
        CloneVmTpmReply reply = new CloneVmTpmReply();

        String originTpmUuid = Q.New(TpmVO.class)
                .eq(TpmVO_.vmInstanceUuid, msg.getSrcVmUuid())
                .select(TpmVO_.uuid)
                .findValue();
        if (originTpmUuid == null) {
            bus.reply(msg, reply);
            return;
        }

        SimpleFlowChain chain = new SimpleFlowChain();
        chain.setName("clone-VM-TPM");
        chain.then(new Flow() {
            String __name__ = "persist-TPM-VO";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                reply.setInventories(new ArrayList<>());
                for (String dstVmUuid : msg.getDstVmUuidList()) {
                    TpmVO dstTpm = vmTpmManager.persistTpmVO(null, dstVmUuid);
                    reply.getInventories().add(TpmInventory.valueOf(dstTpm));
                }
                trigger.next();
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (CollectionUtils.isEmpty(reply.getInventories())) {
                    trigger.rollback();
                    return;
                }
                SQL.New(TpmVO.class)
                        .in(TpmVO_.uuid, transform(reply.getInventories(), TpmInventory::getUuid))
                        .delete();
                trigger.rollback();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "clone-encrypted-resource-key-if-needed";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                boolean resetTpm;
                if (msg.getResetTpm() == null) {
                    ResourceConfig resourceConfig = resourceConfigFacade.getResourceConfig(RESET_TPM_AFTER_VM_CLONE.getIdentity());
                    resetTpm = resourceConfig.getResourceConfigValue(msg.getSrcVmUuid(), Boolean.class);
                } else {
                    resetTpm = msg.getResetTpm();
                }

                new While<>(reply.getInventories()).each((inventory, whileCompletion) -> {
                    TpmEncryptedResourceKeyBackend.CloneEncryptedResourceKeyContext context =
                            new TpmEncryptedResourceKeyBackend.CloneEncryptedResourceKeyContext();
                    context.srcTpmUuid = originTpmUuid;
                    context.dstTpmUuid = inventory.getUuid();
                    context.resetTpm = resetTpm;
                    tpmKeyBackend.cloneEncryptedResourceKey(context, new Completion(whileCompletion) {
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
                        if (errorCodeList.isEmpty()) {
                            trigger.next();
                            return;
                        }
                        trigger.fail(operr("Failed to clone encrypted resource key")
                                .withOpaque("src.tpm.uuid", originTpmUuid)
                                .causedBy(errorCodeList));
                    }
                });
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                bus.reply(msg, reply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                reply.setError(errCode);
                bus.reply(msg, reply);
            }
        }).start();
    }

    private void handle(ResetVmTpmMsg msg) {
        ResetVmTpmReply reply = new ResetVmTpmReply();

        String vmUuid = msg.getVmInstanceUuid();
        new SQLBatch() {
            @Override
            protected void scripts() {
                sql(VmHostFileVO.class)
                        .eq(VmHostFileVO_.vmInstanceUuid, vmUuid)
                        .eq(VmHostFileVO_.type, VmHostFileType.TpmState)
                        .delete();
                sql(VmHostBackupFileVO.class)
                        .eq(VmHostBackupFileVO_.resourceUuid, vmUuid)
                        .eq(VmHostBackupFileVO_.type, VmHostFileType.TpmState)
                        .delete();
            }
        }.execute();

        bus.reply(msg, reply);
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

        List<VmHostFileVO> files = Q.New(VmHostFileVO.class)
                .eq(VmHostFileVO_.vmInstanceUuid, vm.getUuid())
                .in(VmHostFileVO_.type, list(VmHostFileType.TpmState, VmHostFileType.NvRam))
                .list();
        view.setFileRefs(VmHostFileInventory.valueOf(files));

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
