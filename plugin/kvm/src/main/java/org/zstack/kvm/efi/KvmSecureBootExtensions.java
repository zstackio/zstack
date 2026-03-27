package org.zstack.kvm.efi;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.compute.vm.devices.VmTpmManager;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.MessageReply;
import org.zstack.header.tpm.entity.TpmVO;
import org.zstack.header.tpm.entity.TpmVO_;
import org.zstack.header.vm.DiskAO;
import org.zstack.header.vm.PreVmInstantiateResourceExtensionPoint;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.AfterReimageVmInstanceExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceMigrateExtensionPoint;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstantiateResourceException;
import org.zstack.header.vm.VmJustBeforeDeleteFromDbExtensionPoint;
import org.zstack.header.vm.VmMigrationType;
import org.zstack.header.vm.VmPreMigrationExtensionPoint;
import org.zstack.header.vm.VmReleaseResourceExtensionPoint;
import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.vm.additions.VmHostBackupFileVO_;
import org.zstack.header.vm.additions.VmHostFileContentVO;
import org.zstack.header.vm.additions.VmHostFileContentVO_;
import org.zstack.header.vm.additions.VmHostFileOperation;
import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.header.vm.additions.VmHostFileVO;
import org.zstack.header.vm.additions.VmHostFileVO_;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMAgentCommands.*;
import org.zstack.kvm.KVMGlobalConfig;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMStartVmExtensionPoint;
import org.zstack.kvm.KvmCommandSender;
import org.zstack.kvm.KvmResponseWrapper;
import org.zstack.kvm.VolumeTO;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.zstack.compute.vm.VmGlobalConfig.ENABLE_UEFI_SECURE_BOOT;
import static org.zstack.core.Platform.operr;
import static org.zstack.header.vm.VmMigrationType.HostMigration;
import static org.zstack.header.vm.additions.VmHostFileSyncReason.PostMigration;
import static org.zstack.header.vm.additions.VmHostFileSyncReason.PrepareReRead;
import static org.zstack.header.vm.additions.VmHostFileSyncReason.PrepareRead;
import static org.zstack.header.vm.additions.VmHostFileSyncReason.ResourceRelease;
import static org.zstack.kvm.KVMConstant.*;
import static org.zstack.utils.CollectionDSL.list;

public class KvmSecureBootExtensions implements KVMStartVmExtensionPoint,
        PreVmInstantiateResourceExtensionPoint,
        VmJustBeforeDeleteFromDbExtensionPoint,
        VmPreMigrationExtensionPoint,
        AfterReimageVmInstanceExtensionPoint,
        VmReleaseResourceExtensionPoint,
        VmInstanceMigrateExtensionPoint {
    private static final CLogger logger = Utils.getLogger(KvmSecureBootExtensions.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ResourceConfigFacade resourceConfigFacade;
    @Autowired
    private DatabaseFacade databaseFacade;

    private final Object hostFileLock = new Object();

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        if (isUefiBootMode(cmd.getBootMode())) {
            ResourceConfig resourceConfig;
            resourceConfig = resourceConfigFacade.getResourceConfig(VmGlobalConfig.ENABLE_UEFI_SECURE_BOOT.getIdentity());
            cmd.setSecureBoot(resourceConfig.getResourceConfigValue(spec.getVmInventory().getUuid(), Boolean.class));

            resourceConfig = resourceConfigFacade.getResourceConfig(KVMGlobalConfig.VM_EDK_VERSION_CONFIG.getIdentity());
            final String edkVersion = resourceConfig.getResourceConfigValue(spec.getVmInventory().getUuid(), String.class);
            if (!Objects.equals(edkVersion, EDK_VERSION_NONE)) {
                cmd.setEdkVersion(edkVersion);
            }
        }

        if (spec.getNvRamSpec() != null) {
            prepareNvRamToStartVmCmd(cmd, spec.getNvRamSpec(), host);
        }
    }

    private void prepareNvRamToStartVmCmd(KVMAgentCommands.StartVmCmd cmd, DiskAO nvRamSpec, KVMHostInventory host) {
        VolumeTO volume = new VolumeTO();
        volume.setDeviceType(VolumeTO.FILE);
        volume.setInstallPath(buildNvramFilePath(cmd.getVmInstanceUuid()));
        volume.setVolumeUuid(null); // not a volume
        cmd.setNvRam(volume);

        synchronized (hostFileLock) {
            VmHostFileVO nvRamFile = Q.New(VmHostFileVO.class)
                    .eq(VmHostFileVO_.vmInstanceUuid, cmd.getVmInstanceUuid())
                    .eq(VmHostFileVO_.type, VmHostFileType.NvRam)
                    .eq(VmHostFileVO_.hostUuid, host.getUuid())
                    .find();
            if (nvRamFile == null) {
                nvRamFile = new VmHostFileVO();
                nvRamFile.setUuid(Platform.getUuid());
                nvRamFile.setHostUuid(host.getUuid());
                nvRamFile.setVmInstanceUuid(cmd.getVmInstanceUuid());
                nvRamFile.setType(VmHostFileType.NvRam);
                nvRamFile.setPath(volume.getInstallPath());
                nvRamFile.setCreateDate(Timestamp.from(Instant.now()));
                nvRamFile.setResourceName("NvRam file for " + cmd.getVmInstanceUuid());
                databaseFacade.persist(nvRamFile);
            } else {
                SQL.New(VmHostFileVO.class)
                        .eq(VmHostFileVO_.uuid, nvRamFile.getUuid())
                        .set(VmHostFileVO_.path, volume.getInstallPath())
                        .set(VmHostFileVO_.lastOpDate, Timestamp.from(Instant.now()))
                        .update();
            }
        }
    }

    @Override
    public void preVmMigration(VmInstanceInventory vm, VmMigrationType type, String dstHostUuid, Completion completion) {
        if (HostMigration != type) {
            completion.success();
            return;
        }

        String tpmUuid = Q.New(TpmVO.class)
                .eq(TpmVO_.vmInstanceUuid, vm.getUuid())
                .select(TpmVO_.uuid)
                .findValue();
        boolean needRegisterNvRam = tpmUuid != null;
        if (!needRegisterNvRam) {
            String bootMode = VmSystemTags.BOOT_MODE.getTokenByResourceUuid(vm.getUuid(), VmSystemTags.BOOT_MODE_TOKEN);
            if (isUefiBootMode(bootMode)) {
                ResourceConfig resourceConfig = resourceConfigFacade.getResourceConfig(ENABLE_UEFI_SECURE_BOOT.getIdentity());
                needRegisterNvRam = resourceConfig.getResourceConfigValue(vm.getUuid(), Boolean.class) == Boolean.TRUE;
            }

            if (!needRegisterNvRam) {
                completion.success();
                return;
            }
        }

        SimpleFlowChain.of("prepare-nvram-before-vm-" + vm.getUuid() + "-migrate")
                .then("prepare-nvram-folder-on-dest-host", trigger -> {
                    VmHostFileTO to = new VmHostFileTO();
                    to.setPath(buildNvramFilePath(vm.getUuid()));
                    to.setType(VmHostFileType.NvRam.toString());
                    to.setOperation(VmHostFileOperation.Prepare.toString());

                    RewriteVmHostFilesContext context = new RewriteVmHostFilesContext();
                    context.hostUuid = dstHostUuid;
                    context.hostFiles = list(to);

                    rewriteVmHostFiles(context, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                })
                // TpmState folder is not needed to prepare
                .propagateExceptionTo(completion)
                .done(completion::success)
                .error(completion::fail)
                .start();
    }

    public static class RewriteVmHostFilesContext {
        public String hostUuid;
        public List<KVMAgentCommands.VmHostFileTO> hostFiles;
    }

    public void rewriteVmHostFiles(RewriteVmHostFilesContext context, Completion completion) {
        KvmCommandSender sender = new KvmCommandSender(context.hostUuid);
        KVMAgentCommands.WriteVmHostFileContentCmd cmd = new KVMAgentCommands.WriteVmHostFileContentCmd();
        cmd.setHostFiles(context.hostFiles);

        sender.send(cmd, WRITE_VM_HOST_FILE_PATH, wrapper -> {
            KVMAgentCommands.WriteVmHostFileContentResponse writeRsp = wrapper.getResponse(KVMAgentCommands.WriteVmHostFileContentResponse.class);
            return writeRsp.isSuccess() ? null :
                    operr("failed to write file content response").causedBy(writeRsp.getError());
        }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
            @Override
            public void success(KvmResponseWrapper wrapper) {
                KVMAgentCommands.WriteVmHostFileContentResponse writeRsp = wrapper.getResponse(KVMAgentCommands.WriteVmHostFileContentResponse.class);
                if (!writeRsp.isSuccess()) {
                    completion.fail(operr("failed to write file content response").causedBy(writeRsp.getError()));
                    return;
                }
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {
        // do-nothing
    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {
        // do-nothing
    }

    private boolean isUefiBootMode(String bootMode) {
        return VmTpmManager.isUefiBootMode(bootMode);
    }

    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) throws VmInstantiateResourceException {
        // do-nothing
    }

    @Override
    public void preInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        final DiskAO nvRamSpec = spec.getNvRamSpec();
        if (nvRamSpec == null) {
            completion.success();
            return;
        }

        PrepareHostFileContext context = new PrepareHostFileContext();
        context.hostUuid = spec.getDestHost().getUuid();
        context.vmUuid = spec.getVmInventory().getUuid();
        context.type = VmHostFileType.NvRam;
        context.syncReason = "pre-instantiate VM resource";
        prepareHostFileOnHost(context, completion);
    }

    public static class PrepareHostFileContext {
        public String hostUuid;
        public String vmUuid;
        public VmHostFileType type;
        public String backupUuid;
        public String syncReason;

        public String path;
        // whether the NvRam is on the same host as before
        private boolean sameHost = false;
        private boolean writeSuccess = false;
        private VmHostFileVO vmHostFile;
        private VmHostBackupFileVO vmBackupFileVO;

        // property: VmHostBackupFileVO (when "backupUuid" is set)
        //             > VmHostFileVO (read success)
        //             > VmHostFileVO (read fail)
        //             > VmHostBackupFileVO (vmInstanceUuid matched)  <-  read this only if VmHostFileVO is not exist
    }

    @SuppressWarnings("rawtypes")
    public void prepareHostFileOnHost(PrepareHostFileContext context, Completion completion) {
        SimpleFlowChain chain = new SimpleFlowChain();
        chain.setName("prepare-vm-host-file");
        chain.then(new NoRollbackFlow() {
            String __name__ = "read-vm-host-file-from-origin-host";

            @Override
            public boolean skip(Map data) {
                return StringUtils.isNotBlank(context.backupUuid);
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                VmHostFileVO vmHostFile = Q.New(VmHostFileVO.class)
                        .eq(VmHostFileVO_.type, context.type)
                        .eq(VmHostFileVO_.vmInstanceUuid, context.vmUuid)
                        .orderByDesc(VmHostFileVO_.lastOpDate)
                        .limit(1)
                        .find();
                if (vmHostFile == null) {
                    logger.debug(String.format("skip to read/write %s host file for VM[vmUuid=%s]: file is not registered in MN",
                            context.type, context.vmUuid));
                    trigger.next();
                    return;
                }

                context.sameHost = vmHostFile.getHostUuid().equals(context.hostUuid);
                SyncVmHostFilesFromHostMsg syncMsg = new SyncVmHostFilesFromHostMsg();
                syncMsg.setHostUuid(vmHostFile.getHostUuid());
                syncMsg.setVmUuid(context.vmUuid);
                syncMsg.setSyncReason(PrepareRead.reason(context.syncReason));

                if (vmHostFile.getType() == VmHostFileType.NvRam) {
                    context.path = vmHostFile.getPath();
                    syncMsg.setNvRamPath(context.path);
                } else if (vmHostFile.getType() == VmHostFileType.TpmState) {
                    context.path = vmHostFile.getPath();
                    syncMsg.setTpmStateFolder(context.path);
                } else {
                    throw new CloudRuntimeException("unsupported vm host file type: " + vmHostFile.getType());
                }

                bus.makeLocalServiceId(syncMsg, VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
                bus.send(syncMsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            context.vmHostFile = vmHostFile;
                        } else {
                            logger.warn(String.format("failed to read vm host file for VM[vmUuid=%s] but still continue: %s",
                                    context.vmUuid, reply.getError().getReadableDetails()));
                        }
                        trigger.next();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "read-vm-host-file-from-backup";

            @Override
            public boolean skip(Map data) {
                return context.vmHostFile != null;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (context.backupUuid == null) {
                    context.vmBackupFileVO = Q.New(VmHostBackupFileVO.class)
                            .eq(VmHostBackupFileVO_.type, context.type)
                            .eq(VmHostBackupFileVO_.resourceUuid, context.vmUuid)
                            .orderByDesc(VmHostBackupFileVO_.lastOpDate)
                            .limit(1)
                            .find();
                } else {
                    context.vmBackupFileVO = Q.New(VmHostBackupFileVO.class)
                            .eq(VmHostBackupFileVO_.uuid, context.backupUuid)
                            .find();
                    if (context.vmBackupFileVO == null) {
                        trigger.fail(operr("cannot find matched vm-host backup file[backupUuid:%s]",
                                context.backupUuid));
                        return;
                    }
                }
                if (context.vmBackupFileVO != null) {
                    logger.debug(String.format("use %s[type=%s] VM-host backup file for VM[uuid=%s]",
                            context.vmBackupFileVO.getUuid(), context.type, context.vmUuid));
                    context.path = buildPathForVmHostFileType(context.type, context.vmUuid);
                }
                trigger.next();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "write-vm-host-file-to-dest-host";

            @Override
            public boolean skip(Map data) {
                return (context.vmHostFile == null && context.vmBackupFileVO == null)
                        || (context.sameHost && context.vmHostFile != null);
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                String contentUuid = context.vmHostFile == null ?
                        context.vmBackupFileVO.getUuid() : context.vmHostFile.getUuid();
                VmHostFileContentVO content = Q.New(VmHostFileContentVO.class)
                        .eq(VmHostFileContentVO_.uuid, contentUuid)
                        .find();
                if (content == null) {
                    logger.debug(String.format("skip to write vm host file for VM[vmUuid=%s]: file content is not saved in MN",
                            context.vmUuid));
                    trigger.next();
                    return;
                }

                VmHostFileTO to = new VmHostFileTO();
                to.setPath(context.path);
                to.setType(context.type.toString());
                to.setFileFormat(content.getFormat().toString());
                to.setOperation(VmHostFileOperation.Write.toString());

                String contentBase64 = Base64.getEncoder().encodeToString(content.getContent());
                to.setContentBase64(contentBase64);

                RewriteVmHostFilesContext rewriteContext = new RewriteVmHostFilesContext();
                rewriteContext.hostUuid = context.hostUuid;
                rewriteContext.hostFiles = Collections.singletonList(to);

                rewriteVmHostFiles(rewriteContext, new Completion(trigger) {
                    @Override
                    public void success() {
                        context.writeSuccess = true;
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "re-read-vm-host-file-from-dest-host";

            @Override
            public boolean skip(Map data) {
                return !context.writeSuccess;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                SyncVmHostFilesFromHostMsg syncMsg = new SyncVmHostFilesFromHostMsg();
                syncMsg.setHostUuid(context.hostUuid);
                syncMsg.setVmUuid(context.vmUuid);
                syncMsg.setSyncReason(PrepareReRead.reason(context.syncReason));

                if (context.type == VmHostFileType.NvRam) {
                    syncMsg.setNvRamPath(context.path);
                } else if (context.type == VmHostFileType.TpmState) {
                    syncMsg.setTpmStateFolder(context.path);
                }

                bus.makeLocalServiceId(syncMsg, VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
                bus.send(syncMsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            trigger.next();
                        } else {
                            trigger.fail(reply.getError());
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
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        completion.success();
    }

    static class NvRamVolumeContext {
        String vmUuid;
        DiskAO nvRamSpec;
        VmInstanceSpec spec;

        VolumeInventory inventory;
    }

    @Override
    public void vmJustBeforeDeleteFromDb(VmInstanceInventory inv) {
        String vmUuid = inv.getUuid();
        new SQLBatch() {
            @Override
            protected void scripts() {
                sql(VmHostFileVO.class)
                        .eq(VmHostFileVO_.vmInstanceUuid, vmUuid)
                        .delete();
                sql(VmHostBackupFileVO.class)
                        .eq(VmHostBackupFileVO_.resourceUuid, vmUuid)
                        .delete();
            }
        }.execute();
    }

    @Override
    public void afterReimageVmInstance(VolumeInventory inventory) {
        String vmUuid = inventory.getVmInstanceUuid();
        if (vmUuid == null) {
            return;
        }

        SQL.New(VmHostFileVO.class)
                .eq(VmHostFileVO_.vmInstanceUuid, vmUuid)
                .delete();
        SQL.New(VmHostBackupFileVO.class)
                .eq(VmHostBackupFileVO_.resourceUuid, vmUuid)
                .delete();

        logger.debug(String.format("reset TPM state for VM[uuid:%s] after reimage: " +
                "deleted all VmHostFileVO and VmHostBackupFileVO records", vmUuid));
    }

    @Override
    public void releaseVmResource(VmInstanceSpec spec, Completion completion) {
        if (spec.getDestHost() == null) {
            completion.success();
            return;
        }

        String hostUuid = spec.getDestHost().getUuid();
        String vmUuid = spec.getVmInventory().getUuid();
        List<VmHostFileVO> vmHostFiles = Q.New(VmHostFileVO.class)
                .eq(VmHostFileVO_.vmInstanceUuid, vmUuid)
                .eq(VmHostFileVO_.hostUuid, hostUuid)
                .list();
        if (vmHostFiles.isEmpty()) {
            completion.success();
            return;
        }

        logger.info(String.format("try to sync VM host file[vmUuid=%s] from host[uuid=%s]", vmUuid, hostUuid));
        SyncVmHostFilesFromHostMsg syncMsg = new SyncVmHostFilesFromHostMsg();
        syncMsg.setHostUuid(hostUuid);
        syncMsg.setVmUuid(vmUuid);
        syncMsg.setSyncReason(ResourceRelease.reason());

        for (VmHostFileVO file : vmHostFiles) {
            if (file.getType() == VmHostFileType.NvRam) {
                syncMsg.setNvRamPath(file.getPath());
            } else if (file.getType() == VmHostFileType.TpmState) {
                syncMsg.setTpmStateFolder(file.getPath());
            } else {
                logger.warn(String.format("unsupported vm host file type: %s, skip syncing for VM[uuid:%s] from host[uuid:%s]",
                        file.getType(), vmUuid, hostUuid));
            }
        }

        bus.makeLocalServiceId(syncMsg, VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
        bus.send(syncMsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to sync VM host file[vmUuid=%s] from host[uuid=%s] but still continue: %s",
                            vmUuid, hostUuid, reply.getError().getReadableDetails()));
                }
                completion.success();
            }
        });
    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid, NoErrorCompletion completion) {
        String destHostUuid = inv.getHostUuid();
        String vmUuid = inv.getUuid();

        List<VmHostFileVO> vmHostFiles = Q.New(VmHostFileVO.class)
                .eq(VmHostFileVO_.vmInstanceUuid, vmUuid)
                .eq(VmHostFileVO_.hostUuid, srcHostUuid)
                .list();
        if (vmHostFiles.isEmpty()) {
            completion.done();
            return;
        }

        // clean up stale VmHostFileVO/VmHostFileContentVO on dest host
        // before sync creates new records
        List<VmHostFileVO> staleFiles = Q.New(VmHostFileVO.class)
                .eq(VmHostFileVO_.vmInstanceUuid, vmUuid)
                .eq(VmHostFileVO_.hostUuid, destHostUuid)
                .list();
        if (!staleFiles.isEmpty()) {
            List<String> staleUuids = staleFiles.stream()
                    .map(VmHostFileVO::getUuid)
                    .collect(java.util.stream.Collectors.toList());
            SQL.New(VmHostFileContentVO.class)
                    .in(VmHostFileContentVO_.uuid, staleUuids)
                    .delete();
            SQL.New(VmHostFileVO.class)
                    .in(VmHostFileVO_.uuid, staleUuids)
                    .delete();
            logger.debug(String.format("cleaned up %d stale VmHostFileVO/Content on dest host[uuid=%s] for VM[uuid=%s]",
                    staleFiles.size(), destHostUuid, vmUuid));
        }

        logger.info(String.format("sync VM host file[vmUuid=%s] from dest host[uuid=%s] after migration",
                vmUuid, destHostUuid));
        SyncVmHostFilesFromHostMsg syncMsg = new SyncVmHostFilesFromHostMsg();
        syncMsg.setHostUuid(destHostUuid);
        syncMsg.setVmUuid(vmUuid);
        syncMsg.setSyncReason(PostMigration.reason());

        for (VmHostFileVO file : vmHostFiles) {
            if (file.getType() == VmHostFileType.NvRam) {
                syncMsg.setNvRamPath(file.getPath());
            } else if (file.getType() == VmHostFileType.TpmState) {
                syncMsg.setTpmStateFolder(file.getPath());
            } else {
                logger.warn(String.format("unsupported vm host file type: %s, skip syncing for VM[uuid:%s]",
                        file.getType(), vmUuid));
            }
        }

        bus.makeLocalServiceId(syncMsg, VmInstanceConstant.SECURE_BOOT_SERVICE_ID);
        bus.send(syncMsg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to sync VM host file[vmUuid=%s] from host[uuid=%s] after migration, but tolerated: %s",
                            vmUuid, destHostUuid, reply.getError().getReadableDetails()));
                }
                completion.done();
            }
        });
    }
}
