package org.zstack.kvm.efi;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.legacy.ComputeLegacyGlobalProperty;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.compute.vm.devices.VmTpmManager;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.DiskAO;
import org.zstack.header.vm.PreVmInstantiateResourceExtensionPoint;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstantiateResourceException;
import org.zstack.header.vm.additions.VmHostFileContentFormat;
import org.zstack.header.vm.additions.VmHostFileContentVO;
import org.zstack.header.vm.additions.VmHostFileContentVO_;
import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.header.vm.additions.VmHostFileVO;
import org.zstack.header.vm.additions.VmHostFileVO_;
import org.zstack.header.volume.CreateVolumeMsg;
import org.zstack.header.volume.CreateVolumeReply;
import org.zstack.header.volume.DeleteVolumeMsg;
import org.zstack.header.volume.InstantiateVolumeMsg;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeDeletionPolicyManager;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
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

import javax.persistence.Tuple;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.zstack.core.Platform.operr;
import static org.zstack.kvm.KVMConstant.*;
import static org.zstack.utils.CollectionUtils.findOneOrNull;
import static org.zstack.utils.CollectionUtils.transform;

public class KvmSecureBootExtensions implements KVMStartVmExtensionPoint,
        PreVmInstantiateResourceExtensionPoint {
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
        if (ComputeLegacyGlobalProperty.enableNvRamTypeVolume) {
            VolumeVO vo = Q.New(VolumeVO.class)
                    .eq(VolumeVO_.uuid, nvRamSpec.getSourceUuid())
                    .find();
            if (vo == null) {
                if (nvRamSpec.getSourceUuid() != null) {
                    throw new CloudRuntimeException(String.format("cannot find NvRam volume[uuid:%s]", nvRamSpec.getSourceUuid()));
                }
                return;
            }

            VolumeInventory nvRamVolume = VolumeInventory.valueOf(vo);
            VolumeTO volume = VolumeTO.valueOfWithOutExtension(nvRamVolume, host, null);
            cmd.setNvRam(volume);
            return;
        }

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

    public static class SyncVmHostFilesFromHostContext {
        public String hostUuid;
        public String vmUuid;

        public String nvRamPath;
        public String tpmStateFolder;
    }

    public void syncVmHostFilesFromHost(SyncVmHostFilesFromHostContext context, Completion completion) {
        KvmCommandSender sender = new KvmCommandSender(context.hostUuid);

        ReadVmHostFileContentCmd cmd = new ReadVmHostFileContentCmd();
        cmd.setHostFiles(new ArrayList<>());
        if (context.tpmStateFolder != null) {
            VmHostFileTO to = new VmHostFileTO();
            to.setPath(context.tpmStateFolder);
            to.setType(VmHostFileType.TpmState.toString());
            cmd.getHostFiles().add(to);
        }
        if (context.nvRamPath != null) {
            VmHostFileTO to = new VmHostFileTO();
            to.setPath(context.nvRamPath);
            to.setType(VmHostFileType.NvRam.toString());
            cmd.getHostFiles().add(to);
        }

        sender.send(cmd, READ_VM_HOST_FILE_PATH, wrapper -> {
            ReadVmHostFileContentResponse readRsp = wrapper.getResponse(ReadVmHostFileContentResponse.class);
            return readRsp.isSuccess() ? null :
                    operr("failed to read file content response").withException(readRsp.getError());
        }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
            @Override
            public void success(KvmResponseWrapper wrapper) {
                ReadVmHostFileContentResponse readRsp = wrapper.getResponse(ReadVmHostFileContentResponse.class);
                if (!readRsp.isSuccess()) {
                    completion.fail(operr("failed to read file content response").withException(readRsp.getError()));
                    return;
                }

                final List<VmHostFileVO> existsFiles = Q.New(VmHostFileVO.class)
                        .eq(VmHostFileVO_.vmInstanceUuid, context.vmUuid)
                        .eq(VmHostFileVO_.hostUuid, context.hostUuid)
                        .in(VmHostFileVO_.path, cmd.getPaths())
                        .list();
                final List<String> existsContentUuid;
                if (!existsFiles.isEmpty()) {
                    existsContentUuid = Q.New(VmHostFileContentVO.class)
                            .in(VmHostFileContentVO_.uuid, transform(existsFiles, VmHostFileVO::getUuid))
                            .select(VmHostFileContentVO_.uuid)
                            .listValues();
                } else {
                    existsContentUuid = Collections.emptyList();
                }

                for (String path : cmd.getPaths()) {
                    VmHostFileTO to = findOneOrNull(readRsp.getHostFiles(), item -> item.getPath().equals(path));
                    if (to == null) {
                        continue;
                    }
                    if (to.getError() != null) {
                        logger.warn(String.format("failed to read file content from host[uuid=%s] with file %s: %s",
                                context.hostUuid, path, to.getError()));
                        continue;
                    }

                    VmHostFileType type = Objects.equals(path, context.nvRamPath) ?
                            VmHostFileType.NvRam : VmHostFileType.TpmState;

                    VmHostFileVO file = findOneOrNull(existsFiles, item -> item.getPath().equals(path));
                    boolean fileExists = file != null;

                    Timestamp now = Timestamp.from(Instant.now());
                    if (fileExists) {
                        SQL.New(VmHostFileVO.class)
                                .eq(VmHostFileVO_.uuid, file.getUuid())
                                .set(VmHostFileVO_.lastOpDate, now)
                                .update();
                    } else {
                        file = new VmHostFileVO();
                        file.setUuid(Platform.getUuid());
                        file.setHostUuid(context.hostUuid);
                        file.setVmInstanceUuid(context.vmUuid);
                        file.setPath(path);
                        file.setType(type);
                        file.setCreateDate(now);
                        file.setLastOpDate(now);
                        file.setResourceName(String.format("%s file for %s", type, context.vmUuid));
                        databaseFacade.persist(file);
                    }

                    byte[] bytes = Base64.getDecoder().decode(to.getContentBase64());
                    if (existsContentUuid.contains(file.getUuid())) {
                        SQL.New(VmHostFileContentVO.class)
                                .eq(VmHostFileContentVO_.uuid, file.getUuid())
                                .set(VmHostFileContentVO_.content, bytes)
                                .set(VmHostFileContentVO_.format, VmHostFileContentFormat.valueOf(to.getFileFormat()))
                                .set(VmHostFileContentVO_.lastOpDate, now)
                                .update();
                    } else {
                        VmHostFileContentVO content = new VmHostFileContentVO();
                        content.setUuid(file.getUuid());
                        content.setContent(bytes);
                        content.setFormat(VmHostFileContentFormat.valueOf(to.getFileFormat()));
                        content.setCreateDate(now);
                        content.setLastOpDate(now);
                        databaseFacade.persist(content);
                    }
                }

                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
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
                    operr("failed to write file content response").withException(writeRsp.getError());
        }, new ReturnValueCompletion<KvmResponseWrapper>(completion) {
            @Override
            public void success(KvmResponseWrapper wrapper) {
                KVMAgentCommands.WriteVmHostFileContentResponse writeRsp = wrapper.getResponse(KVMAgentCommands.WriteVmHostFileContentResponse.class);
                if (!writeRsp.isSuccess()) {
                    completion.fail(operr("failed to write file content response").withException(writeRsp.getError()));
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
        if (ComputeLegacyGlobalProperty.enableNvRamTypeVolume) {
            prepareNvRamVolumeOnHost(spec, completion);
        } else {
            prepareNvRamHostFileOnHost(spec, completion);
        }
    }

    private void prepareNvRamVolumeOnHost(VmInstanceSpec spec, Completion completion) {
        final DiskAO nvRamSpec = spec.getNvRamSpec();
        boolean needRegisterNvRam = nvRamSpec != null;

        Tuple tuple = Q.New(VolumeVO.class)
                .eq(VolumeVO_.vmInstanceUuid, spec.getVmInventory().getUuid())
                .eq(VolumeVO_.type, VolumeType.NvRam)
                .select(VolumeVO_.uuid, VolumeVO_.status)
                .findTuple();

        String nvRamVolumeUuid = tuple == null ? null : tuple.get(0, String.class);
        if (needRegisterNvRam && nvRamVolumeUuid != null) {
            nvRamSpec.setSourceUuid(nvRamVolumeUuid);

            VolumeStatus volumeStatus = tuple.get(1, VolumeStatus.class);
            if (volumeStatus != VolumeStatus.Ready) {
                completion.fail(operr("NvRam volume[uuid:%s] is not ready", nvRamVolumeUuid));
                return;
            }

            completion.success();
            return;
        } else if (!needRegisterNvRam && nvRamVolumeUuid == null) {
            completion.success();
            return;
        } else if (needRegisterNvRam) {
            nvRamSpec.setPrimaryStorageUuid(Q.New(VolumeVO.class)
                    .eq(VolumeVO_.type, VolumeType.Root)
                    .eq(VolumeVO_.vmInstanceUuid, spec.getVmInventory().getUuid())
                    .select(VolumeVO_.primaryStorageUuid)
                    .findValue());

            NvRamVolumeContext context = new NvRamVolumeContext();
            context.vmUuid = spec.getVmInventory().getUuid();
            context.nvRamSpec = nvRamSpec;
            context.spec = spec;
            createNvRamVolume(context, new Completion(completion) {
                @Override
                public void success() {
                    nvRamSpec.setSourceUuid(context.inventory.getUuid());
                    completion.success();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    completion.fail(errorCode);
                }
            });
            return;
        }

        deleteNvRamVolumeIfExists(spec.getVmInventory().getUuid(), new Completion(completion) {
            @Override
            public void success() {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn("failed to delete NvRam but still continue: " + errorCode.getReadableDetails());
                completion.success();
            }
        });
    }

    public static class PrepareHostFileContext {
        public String hostUuid;
        public String vmUuid;
        public VmHostFileType type;

        // whether the NvRam is on the same host as before
        private boolean sameHost = false;
        private VmHostFileVO vmHostFile;
    }

    @SuppressWarnings("rawtypes")
    public void prepareHostFileOnHost(PrepareHostFileContext context, Completion completion) {
        SimpleFlowChain chain = new SimpleFlowChain();
        chain.setName("prepare-vm-host-file");
        chain.then(new NoRollbackFlow() {
            String __name__ = "read-vm-host-file-from-origin-host";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                VmHostFileVO vmHostFile = context.vmHostFile = Q.New(VmHostFileVO.class)
                        .eq(VmHostFileVO_.type, context.type)
                        .eq(VmHostFileVO_.vmInstanceUuid, context.vmUuid)
                        .orderByDesc(VmHostFileVO_.lastOpDate)
                        .limit(1)
                        .find();
                context.sameHost = vmHostFile != null && vmHostFile.getHostUuid().equals(context.hostUuid);
                if (context.sameHost) {
                    logger.debug(String.format("skip to read/write %s host file for VM[vmUuid=%s]: vm.host is not changed",
                            context.type, context.vmUuid));
                    trigger.next();
                    return;
                }

                if (vmHostFile == null) {
                    logger.debug(String.format("skip to read/write %s host file for VM[vmUuid=%s]: file is not registered in MN",
                            context.type, context.vmUuid));
                    trigger.next();
                    return;
                }

                SyncVmHostFilesFromHostContext syncContext = new SyncVmHostFilesFromHostContext();
                syncContext.hostUuid = vmHostFile.getHostUuid();
                syncContext.vmUuid = context.vmUuid;

                if (vmHostFile.getType() == VmHostFileType.NvRam) {
                    syncContext.nvRamPath = vmHostFile.getPath();
                } else if (vmHostFile.getType() == VmHostFileType.TpmState) {
                    syncContext.tpmStateFolder = vmHostFile.getPath();
                } else {
                    throw new CloudRuntimeException("unsupported vm host file type: " + vmHostFile.getType());
                }

                syncVmHostFilesFromHost(syncContext, new Completion(trigger) {
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
            String __name__ = "write-vm-host-file-to-dest-host";

            @Override
            public boolean skip(Map data) {
                return context.sameHost || context.vmHostFile == null;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                VmHostFileContentVO content = Q.New(VmHostFileContentVO.class)
                        .eq(VmHostFileContentVO_.uuid, context.vmHostFile.getUuid())
                        .find();
                if (content == null) {
                    logger.debug(String.format("skip to write vm host file for VM[vmUuid=%s]: file content is not saved in MN",
                            context.vmUuid));
                    trigger.next();
                    return;
                }

                VmHostFileTO to = new VmHostFileTO();
                to.setPath(context.vmHostFile.getPath());
                to.setType(context.vmHostFile.getType().toString());
                to.setFileFormat(content.getFormat().toString());

                String contentBase64 = Base64.getEncoder().encodeToString(content.getContent());
                to.setContentBase64(contentBase64);

                RewriteVmHostFilesContext rewriteContext = new RewriteVmHostFilesContext();
                rewriteContext.hostUuid = context.hostUuid;
                rewriteContext.hostFiles = Collections.singletonList(to);

                rewriteVmHostFiles(rewriteContext, new Completion(trigger) {
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
            String __name__ = "re-read-vm-host-file-from-dest-host";

            @Override
            public boolean skip(Map data) {
                // if context.sameHost is true, we also need to re-read the host file for cache.
                return context.vmHostFile == null;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                KvmSecureBootExtensions.SyncVmHostFilesFromHostContext syncBackContext =
                        new KvmSecureBootExtensions.SyncVmHostFilesFromHostContext();
                syncBackContext.hostUuid = context.hostUuid;
                syncBackContext.vmUuid = context.vmUuid;

                if (context.type == VmHostFileType.NvRam) {
                    syncBackContext.nvRamPath = context.vmHostFile.getPath();
                } else if (context.type == VmHostFileType.TpmState) {
                    syncBackContext.tpmStateFolder = context.vmHostFile.getPath();
                }

                syncVmHostFilesFromHost(syncBackContext, new Completion(trigger) {
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

    private void prepareNvRamHostFileOnHost(VmInstanceSpec spec, Completion completion) {
        final DiskAO nvRamSpec = spec.getNvRamSpec();
        if (nvRamSpec == null) {
            completion.success();
            return;
        }

        PrepareHostFileContext context = new PrepareHostFileContext();
        context.hostUuid = spec.getDestHost().getUuid();
        context.vmUuid = spec.getVmInventory().getUuid();
        context.type = VmHostFileType.NvRam;
        prepareHostFileOnHost(context, completion);
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

    @SuppressWarnings("rawtypes")
    private void createNvRamVolume(NvRamVolumeContext context, Completion completion) {
        SimpleFlowChain chain = new SimpleFlowChain();
        chain.setChainName("create-nv-ram-volume-for-vm-" + context.vmUuid);
        chain.then(new Flow() {
            String __name__ = "create-nv-ram-volume";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                String accountUuid = Q.New(AccountResourceRefVO.class)
                        .eq(AccountResourceRefVO_.resourceUuid, context.vmUuid)
                        .select(AccountResourceRefVO_.accountUuid)
                        .findValue();

                CreateVolumeMsg msg = new CreateVolumeMsg();
                msg.setAccountUuid(accountUuid);
                msg.setSize(context.nvRamSpec.getSize());
                msg.setVmInstanceUuid(context.vmUuid);
                msg.setPrimaryStorageUuid(context.nvRamSpec.getPrimaryStorageUuid());

                // NvRam file is raw type (*.fd) in libvirt 8.0.0
                // and qcow2 in libvirt 8.1.0+ (soon)
                // We store it as file system (*.raw) with XFS format
                msg.setFormat(VolumeConstant.VOLUME_FORMAT_RAW);
                msg.setName(context.nvRamSpec.getName());
                msg.setVolumeType(VolumeType.NvRam.toString());

                bus.makeLocalServiceId(msg, VolumeConstant.SERVICE_ID);
                bus.send(msg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            CreateVolumeReply castReply = reply.castReply();
                            context.inventory = castReply.getInventory();
                            trigger.next();
                            return;
                        }
                        trigger.fail(operr("failed to create NvRam volume")
                                .withOpaque("vm.uuid", context.vmUuid)
                                .withCause(reply.getError()));
                    }
                });
            }

            @Override
            public void rollback(FlowRollback trigger, Map data) {
                deleteNvRamVolumeIfExists(context.vmUuid, new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.rollback();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn("failed to delete NvRam but still continue: " + errorCode.getReadableDetails());
                        trigger.rollback();
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "instantiate-nvram-volume";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                InstantiateVolumeMsg msg = new InstantiateVolumeMsg();
                msg.setHostUuid(context.spec.getDestHost().getUuid());
                msg.setPrimaryStorageUuid(context.nvRamSpec.getPrimaryStorageUuid());
                msg.setVolumeUuid(context.inventory.getUuid());

                bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, msg.getVolumeUuid());
                bus.send(msg, new CloudBusCallBack(completion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            trigger.next();
                            return;
                        }
                        trigger.fail(operr("failed to instantiate NvRam volume")
                                .withOpaque("vm.uuid", context.vmUuid)
                                .withCause(reply.getError()));
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

    private void deleteNvRamVolumeIfExists(String vmUuid, Completion completion) {
        String volumeUuid = Q.New(VolumeVO.class)
                .eq(VolumeVO_.vmInstanceUuid, vmUuid)
                .eq(VolumeVO_.type, VolumeType.NvRam)
                .select(VolumeVO_.uuid)
                .findValue();
        if (volumeUuid == null) {
            completion.success();
            return;
        }

        DeleteVolumeMsg msg = new DeleteVolumeMsg();
        msg.setDetachBeforeDeleting(false);
        msg.setUuid(volumeUuid);
        msg.setDeletionPolicy(VolumeDeletionPolicyManager.VolumeDeletionPolicy.Direct.toString());
        bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, volumeUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                    return;
                }
                completion.fail(operr("failed to delete NvRam volume")
                        .withOpaque("vm.uuid", vmUuid)
                        .withOpaque("volume.uuid", volumeUuid)
                        .withCause(reply.getError()));
            }
        });
    }
}
