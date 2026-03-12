package org.zstack.kvm.efi;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.legacy.ComputeLegacyGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacadeImpl;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.Message;
import org.zstack.header.tpm.entity.TpmVO;
import org.zstack.header.tpm.entity.TpmVO_;
import org.zstack.header.vm.VmCanonicalEvents;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.vm.additions.VmHostBackupFileVO_;
import org.zstack.header.vm.additions.VmHostFileContentVO;
import org.zstack.header.vm.additions.VmHostFileContentVO_;
import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.header.vm.additions.VmHostFileVO;
import org.zstack.header.vm.additions.VmHostFileVO_;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.compute.vm.VmGlobalConfig.ENABLE_UEFI_SECURE_BOOT;
import static org.zstack.compute.vm.VmGlobalConfig.RESET_TPM_AFTER_VM_CLONE;
import static org.zstack.kvm.efi.KvmSecureBootExtensions.*;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.findOneOrNull;
import static org.zstack.utils.CollectionUtils.transform;

public class KvmSecureBootManager extends AbstractService {
    private static final CLogger logger = Utils.getLogger(KvmSecureBootManager.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private EventFacadeImpl eventFacade;
    @Autowired
    private ResourceConfigFacade resourceConfigFacade;
    @Autowired
    private KvmSecureBootExtensions secureBootExtensions;

    @Override
    public boolean start() {
        setupCanonicalEvents();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @SuppressWarnings("rawtypes")
    private void setupCanonicalEvents() {
        eventFacade.on(VmCanonicalEvents.VM_LIBVIRT_REPORT_SHUTDOWN, new EventCallback<Object>() {
            @Override
            protected void run(Map tokens, Object data) {
                if (ComputeLegacyGlobalProperty.enableNvRamTypeVolume) {
                    return;
                }

                String vmUuid = (String) data;
                Tuple tuple = Q.New(VmInstanceVO.class)
                        .select(VmInstanceVO_.hostUuid, VmInstanceVO_.lastHostUuid)
                        .eq(VmInstanceVO_.uuid, vmUuid)
                        .findTuple();
                if (tuple == null) {
                    return;
                }

                String hostUuid = (String) tuple.get(0);
                if (hostUuid == null) {
                    hostUuid = (String) tuple.get(1);
                }

                List<VmHostFileVO> hostFiles = Q.New(VmHostFileVO.class)
                        .eq(VmHostFileVO_.vmInstanceUuid, vmUuid)
                        .eq(VmHostFileVO_.hostUuid, hostUuid)
                        .in(VmHostFileVO_.type, list(VmHostFileType.NvRam, VmHostFileType.TpmState))
                        .list();
                if (hostFiles.isEmpty()) {
                    return;
                }

                VmHostFileVO nvRamFile = findOneOrNull(hostFiles, it -> it.getType() == VmHostFileType.NvRam);
                VmHostFileVO tpmStateFile = findOneOrNull(hostFiles, it -> it.getType() == VmHostFileType.TpmState);
                if (nvRamFile == null && tpmStateFile == null) {
                    return;
                }

                KvmSecureBootExtensions.SyncVmHostFilesFromHostContext context = new KvmSecureBootExtensions.SyncVmHostFilesFromHostContext();
                context.hostUuid = hostUuid;
                context.vmUuid = vmUuid;
                context.nvRamPath = nvRamFile == null ? null : nvRamFile.getPath();
                context.tpmStateFolder = tpmStateFile == null ? null : tpmStateFile.getPath();
                secureBootExtensions.syncVmHostFilesFromHost(context, new Completion(null) {
                    @Override
                    public void success() {
                        logger.info(String.format("success to read file content from host[uuid=%s]", context.hostUuid));
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format("failed to read file content from host[uuid=%s]: %s",
                                context.hostUuid, errorCode.getReadableDetails()));
                    }
                });
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof CloneVmHostFileMsg) {
            handle((CloneVmHostFileMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    static class CloneVmHostFileContext {
        List<VmHostFileType> typesNeedClone = new ArrayList<>();
        List<VmHostFileVO> files = new ArrayList<>();
        List<VmHostBackupFileVO> backupFiles = new ArrayList<>();
        List<SyncVmHostFilesFromHostContext> syncContexts = new ArrayList<>();
    }

    @SuppressWarnings("rawtypes")
    private void handle(CloneVmHostFileMsg msg) {
        CloneVmHostFileReply reply = new CloneVmHostFileReply();

        boolean hasTpm = Q.New(TpmVO.class)
                .eq(TpmVO_.vmInstanceUuid, msg.getSrcVmUuid())
                .isExists();
        ResourceConfig resourceConfig = resourceConfigFacade.getResourceConfig(ENABLE_UEFI_SECURE_BOOT.getIdentity());
        boolean secureBoot = resourceConfig.getResourceConfigValue(msg.getSrcVmUuid(), Boolean.class);
        if (!hasTpm && !secureBoot) {
            bus.reply(msg, reply);
            return;
        }

        CloneVmHostFileContext context = new CloneVmHostFileContext();
        context.typesNeedClone.add(VmHostFileType.NvRam);
        if (hasTpm) {
            boolean resetTpm;
            if (msg.getResetTpm() == null) {
                resourceConfig = resourceConfigFacade.getResourceConfig(RESET_TPM_AFTER_VM_CLONE.getIdentity());
                resetTpm = resourceConfig.getResourceConfigValue(msg.getSrcVmUuid(), Boolean.class);
            } else {
                resetTpm = msg.getResetTpm();
            }
            if (!resetTpm) {
                context.typesNeedClone.add(VmHostFileType.TpmState);
            }
        }
        logger.debug(String.format("clone VM[uuid=%s] host files for types: %s", msg.getSrcVmUuid(), context.typesNeedClone));

        SimpleFlowChain chain = new SimpleFlowChain();
        chain.setName("clone-vm-host-file");
        chain.then(new NoRollbackFlow() {
            String __name__ = "prepare-sync-vm-host-file-context-list";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                for (VmHostFileType type : context.typesNeedClone) {
                    VmHostFileVO file = Q.New(VmHostFileVO.class)
                            .eq(VmHostFileVO_.vmInstanceUuid, msg.getSrcVmUuid())
                            .eq(VmHostFileVO_.type, type)
                            .orderByDesc(VmHostFileVO_.lastOpDate)
                            .limit(1)
                            .find();
                    if (file == null) {
                        logger.debug(String.format("skip to read/write %s host file for VM[vmUuid=%s]: file is not registered in MN",
                                type, msg.getSrcVmUuid()));
                        continue;
                    }
                    context.files.add(file);
                }

                if (context.files.isEmpty()) {
                    trigger.next();
                    return;
                }

                Map<String, SyncVmHostFilesFromHostContext> contextMap = new HashMap<>();
                for (VmHostFileVO file : context.files) {
                    contextMap.computeIfAbsent(file.getHostUuid(), hostUuid -> {
                        SyncVmHostFilesFromHostContext syncContext = new SyncVmHostFilesFromHostContext();
                        syncContext.hostUuid = hostUuid;
                        syncContext.vmUuid = msg.getSrcVmUuid();
                        return syncContext;
                    });
                }
                context.syncContexts.addAll(contextMap.values());

                for (VmHostFileVO file : context.files) {
                    SyncVmHostFilesFromHostContext syncContext = contextMap.get(file.getHostUuid());
                    if (file.getType() == VmHostFileType.NvRam) {
                        syncContext.nvRamPath = file.getPath();
                    } else if (file.getType() == VmHostFileType.TpmState) {
                        syncContext.tpmStateFolder = file.getPath();
                    } else {
                        throw new CloudRuntimeException("unsupported vm host file type: " + file.getType());
                    }
                }

                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "read-vm-host-file-from-origin-host";

            @Override
            public boolean skip(Map data) {
                return context.syncContexts.isEmpty();
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                new While<>(context.syncContexts).each((syncContext, whileContext) ->
                    secureBootExtensions.syncVmHostFilesFromHost(syncContext, new Completion(whileContext) {
                        @Override
                        public void success() {
                            whileContext.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            whileContext.addError(errorCode);
                            whileContext.done();
                        }
                    })
                ).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        if (!errorCodeList.isEmpty()) {
                            logger.warn(String.format("failed to sync host file for VM[uuid=%s] but still continue:\n%s",
                                    msg.getSrcVmUuid(),
                                    String.join("\n", transform(errorCodeList.getCauses(), ErrorCode::getReadableDetails))));
                        }
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "determine-content-uuid";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<VmHostFileType> missingTypes = new ArrayList<>(context.typesNeedClone);
                missingTypes.removeAll(transform(context.files, VmHostFileVO::getType));
                if (missingTypes.isEmpty()) {
                    trigger.next();
                    return;
                }

                context.backupFiles.addAll(Q.New(VmHostBackupFileVO.class)
                        .eq(VmHostBackupFileVO_.vmInstanceUuid, msg.getSrcVmUuid())
                        .in(VmHostFileVO_.type, missingTypes)
                        .list());
                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "copy-host-content-database";

            @Override
            public boolean skip(Map data) {
                return context.files.isEmpty() && context.backupFiles.isEmpty();
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<String> uuidList = transform(context.files, VmHostFileVO::getUuid);
                List<VmHostFileVO> filesAfterSyncing = Q.New(VmHostFileVO.class)
                        .in(VmHostFileVO_.uuid, uuidList)
                        .list();
                uuidList.addAll(transform(context.backupFiles, VmHostBackupFileVO::getUuid));
                List<VmHostFileContentVO> contents = Q.New(VmHostFileContentVO.class)
                        .in(VmHostFileContentVO_.uuid, uuidList)
                        .list();

                List<VmHostBackupFileVO> filesNeedPersists = new ArrayList<>();
                List<VmHostFileContentVO> contentsNeedPersists = new ArrayList<>();

                Timestamp now = Timestamp.from(Instant.now());
                for (String vmUuid : msg.getDstVmUuidList()) {
                    for (String uuid : uuidList) {
                        VmHostFileContentVO srcContent = findOneOrNull(contents,
                                item -> item.getUuid().equals(uuid));
                        if (srcContent == null) {
                            continue;
                        }

                        VmHostFileVO vmHostFile = findOneOrNull(filesAfterSyncing,
                                item -> item.getUuid().equals(uuid));
                        VmHostBackupFileVO vmHostBackupFile = vmHostFile == null ?
                                findOneOrNull(context.backupFiles, item -> item.getUuid().equals(uuid)) : null;
                        DebugUtils.Assert(vmHostFile != null || vmHostBackupFile != null,
                                "vmHostFile or vmHostBackupFile cannot be null");

                        VmHostBackupFileVO file = new VmHostBackupFileVO();
                        file.setUuid(Platform.getUuid());
                        file.setVmInstanceUuid(vmUuid);
                        file.setType(vmHostFile == null ? vmHostBackupFile.getType() : vmHostFile.getType());
                        file.setCreateDate(now);
                        file.setLastOpDate(now);
                        filesNeedPersists.add(file);

                        VmHostFileContentVO content = new VmHostFileContentVO();
                        content.setUuid(file.getUuid());
                        content.setContent(srcContent.getContent());
                        content.setFormat(srcContent.getFormat());
                        content.setCreateDate(now);
                        content.setLastOpDate(now);
                        contentsNeedPersists.add(content);
                    }
                }

                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("persist VmHostFileContentVO [uuid=%s]",
                            transform(contentsNeedPersists, VmHostFileContentVO::getUuid)));
                }

                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        if (!filesNeedPersists.isEmpty()) {
                            databaseFacade.persistCollection(filesNeedPersists);
                        }
                        if (!contentsNeedPersists.isEmpty()) {
                            databaseFacade.persistCollection(contentsNeedPersists);
                        }
                    }
                }.execute();

                trigger.next();
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
}
