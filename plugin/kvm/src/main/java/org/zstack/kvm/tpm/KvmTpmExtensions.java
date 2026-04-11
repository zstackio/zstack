package org.zstack.kvm.tpm;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.compute.vm.devices.TpmEncryptedResourceKeyBackend;
import org.zstack.compute.vm.devices.VmTpmManager;
import org.zstack.compute.vm.devices.TpmEncryptedResourceKeyBackend.CloneEncryptedResourceKeyContext;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.keyprovider.EncryptedResourceKeyManager;
import org.zstack.header.keyprovider.EncryptedResourceKeyManager.GetOrCreateResourceKeyContext;
import org.zstack.header.keyprovider.EncryptedResourceKeyManager.ResourceKeyResult;
import org.zstack.header.secret.SecretHostDeleteMsg;
import org.zstack.header.secret.SecretHostDefineMsg;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO_;
import org.zstack.header.secret.SecretHostGetMsg;
import org.zstack.header.secret.SecretHostGetReply;
import org.zstack.header.tpm.entity.TpmSpec;
import org.zstack.header.tpm.entity.TpmVO;
import org.zstack.header.tpm.entity.TpmVO_;
import org.zstack.header.vm.HaStartVmInstanceMsg;
import org.zstack.header.vm.VmAfterExpungeExtensionPoint;
import org.zstack.header.vm.VmInstanceMigrateExtensionPoint;
import org.zstack.header.secret.SecretHostDefineReply;
import org.zstack.header.tpm.message.RemoveTpmMsg;
import org.zstack.header.vm.PreVmInstantiateResourceExtensionPoint;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstantiateResourceException;
import org.zstack.header.vm.VmStateChangedExtensionPoint;
import org.zstack.header.vm.VmJustBeforeDeleteFromDbExtensionPoint;
import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.vm.additions.VmHostBackupFileVO_;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.header.vm.additions.VmHostFileVO;
import org.zstack.header.vm.additions.VmHostFileVO_;
import org.zstack.header.vm.devices.VmDevicesSpec;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMStartVmExtensionPoint;
import org.zstack.kvm.efi.KvmSecureBootExtensions;
import org.zstack.kvm.efi.KvmSecureBootExtensions.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import static org.zstack.header.tpm.TpmConstants.SERVICE_ID;
import static org.zstack.kvm.KVMConstant.*;
import static org.zstack.core.Platform.operr;

public class KvmTpmExtensions implements KVMStartVmExtensionPoint,
        PreVmInstantiateResourceExtensionPoint,
        VmInstanceMigrateExtensionPoint,
        VmAfterExpungeExtensionPoint,
        VmStateChangedExtensionPoint,
        VmJustBeforeDeleteFromDbExtensionPoint {
    private static final CLogger logger = Utils.getLogger(KvmTpmExtensions.class);

    @Autowired
    private KvmSecureBootExtensions secureBootExtensions;
    @Autowired
    private DatabaseFacade databaseFacade;
    @Autowired
    private TpmEncryptedResourceKeyBackend resourceKeyBackend;
    @Autowired
    private EncryptedResourceKeyManager resourceKeyManager;
    @Autowired
    private CloudBus bus;

    private final Object hostFileLock = new Object();
    private final Map<String, String> volumeMigratingSourceHostCache = new ConcurrentHashMap<>();

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        final VmDevicesSpec devicesSpec = spec.getDevicesSpec();
        if (devicesSpec == null || devicesSpec.getTpm() == null || !devicesSpec.getTpm().isEnable()) {
            return;
        }

        String keyProviderUuid = devicesSpec.getTpm().getKeyProviderUuid();
        if (StringUtils.isBlank(keyProviderUuid)) {
            keyProviderUuid = resourceKeyBackend.findKeyProviderUuidByTpm(devicesSpec.getTpm().getTpmUuid());
        }

        TpmTO tpm = new TpmTO();
        tpm.setKeyProviderUuid(keyProviderUuid);
        tpm.setSecretUuid(devicesSpec.getTpm().getSecretUuid());
        tpm.setInstallPath(buildTpmStateFilePath(cmd.getVmInstanceUuid()));
        cmd.setTpm(tpm);

        synchronized (hostFileLock) {
            VmHostFileVO tpmStateFile = Q.New(VmHostFileVO.class)
                    .eq(VmHostFileVO_.vmInstanceUuid, cmd.getVmInstanceUuid())
                    .eq(VmHostFileVO_.type, VmHostFileType.TpmState)
                    .eq(VmHostFileVO_.hostUuid, host.getUuid())
                    .find();
            if (tpmStateFile == null) {
                tpmStateFile = new VmHostFileVO();
                tpmStateFile.setUuid(Platform.getUuid());
                tpmStateFile.setHostUuid(host.getUuid());
                tpmStateFile.setVmInstanceUuid(cmd.getVmInstanceUuid());
                tpmStateFile.setType(VmHostFileType.TpmState);
                tpmStateFile.setPath(tpm.getInstallPath());
                tpmStateFile.setCreateDate(Timestamp.from(Instant.now()));
                tpmStateFile.setResourceName("TpmState file for " + cmd.getVmInstanceUuid());
                databaseFacade.persist(tpmStateFile);
            } else {
                SQL.New(VmHostFileVO.class)
                        .eq(VmHostFileVO_.uuid, tpmStateFile.getUuid())
                        .set(VmHostFileVO_.path, tpm.getInstallPath())
                        .set(VmHostFileVO_.lastOpDate, Timestamp.from(Instant.now()))
                        .update();
            }
        }
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {
        if (spec.getMessage() instanceof HaStartVmInstanceMsg) {
            String vmUuid = spec.getVmInventory() == null ? null : spec.getVmInventory().getUuid();
            String srcHostUuid = spec.getVmInventory() == null ? null : spec.getVmInventory().getLastHostUuid();
            Integer keyVersion = findTpmKeyVersionByVmUuid(vmUuid);
            boolean vmIsOnDestHost = isVmCurrentlyOnExpectedHost(vmUuid, host.getUuid());
            if (vmIsOnDestHost && StringUtils.isNotBlank(srcHostUuid) && !host.getUuid().equals(srcHostUuid)) {
                deleteHostSecretBestEffort(srcHostUuid, vmUuid, keyVersion,
                        "ha-start-success");
            }
        }
        clearRollbackInfo(spec);
    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {
        clearRollbackInfo(spec);
    }

    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) throws VmInstantiateResourceException {
        // do-nothing
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void preInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        final VmDevicesSpec devicesSpec = spec.getDevicesSpec();
        if (devicesSpec == null || devicesSpec.getTpm() == null || !devicesSpec.getTpm().isEnable()) {
            completion.success();
            return;
        }

        TpmSpec tpmSpec = devicesSpec.getTpm();
        clearRollbackInfo(spec);
        final PrepareTpmResourceContext context = new PrepareTpmResourceContext();
        context.tpmUuid = tpmSpec.getTpmUuid();
        context.backupFileUuid = tpmSpec.getBackupFileUuid(); // maybe null
        context.providerUuid = resourceKeyBackend.findKeyProviderUuidByTpm(context.tpmUuid);
        context.keyVersion = resourceKeyBackend.findKeyVersionByTpm(context.tpmUuid);
        context.instantiateForNewVm = spec.getCurrentVmOperation() == VmInstanceConstant.VmOperation.NewCreate;
        context.enableKeyProvider = !VmGlobalConfig.ALLOWED_TPM_VM_WITHOUT_KMS.value(Boolean.class);

        final SimpleFlowChain chain = new SimpleFlowChain();
        chain.setName("prepare-tpm-resources-for-vm-" + spec.getVmInventory().getUuid());
        chain.then(new NoRollbackFlow() {
            String __name__ = "prepare-tpm-state-file-on-host";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                PrepareHostFileContext innerContext = new PrepareHostFileContext();
                innerContext.hostUuid = spec.getDestHost().getUuid();
                innerContext.vmUuid = spec.getVmInventory().getUuid();
                innerContext.type = VmHostFileType.TpmState;
                innerContext.backupUuid = context.backupFileUuid;
                innerContext.syncReason = "pre-instantiate VM resource";
                secureBootExtensions.prepareHostFileOnHost(innerContext, new Completion(trigger) {
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
        }).then(new Flow() {
            String __name__ = "clone-tpm-resource-key-from-snapshot-source";

            @Override
            public boolean skip(Map data) {
                return !context.enableKeyProvider;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                String srcTpmUuid = findSourceTpmUuidFromSnapshotTpmBackupFile(context.backupFileUuid);
                if (StringUtils.isBlank(srcTpmUuid)) {
                    trigger.next();
                    return;
                }
                CloneEncryptedResourceKeyContext cloneCtx = new CloneEncryptedResourceKeyContext();
                cloneCtx.srcTpmUuid = srcTpmUuid;
                cloneCtx.dstTpmUuid = context.tpmUuid;
                cloneCtx.resetTpm = false;
                resourceKeyBackend.cloneEncryptedResourceKey(cloneCtx, new Completion(trigger) {
                    @Override
                    public void success() {
                        context.providerUuid = resourceKeyBackend.findKeyProviderUuidByTpm(context.tpmUuid);
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }

            // Use clone op above and will not set rollback flag TpmSpec.resourceKeyCreatedNew
            // to true, so use flow rollback instead preReleaseVmResource rollback. And we
            // definitely don't need to delete keytool secret on shapshot case.
            @Override
            public void rollback(FlowRollback trigger, Map data) {
                if (StringUtils.isNotBlank(context.backupFileUuid)) {
                    try {
                        resourceKeyBackend.detachKeyProviderFromTpm(context.tpmUuid);
                    } catch (Exception e) {
                        logger.warn(String.format("failed to detach key provider ref for tpm[uuid:%s]: %s",
                                context.tpmUuid, e.getMessage()));
                    }
                }
                trigger.rollback();
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "get-secret-on-host-first";

            @Override
            public boolean skip(Map data) {
                return !context.enableKeyProvider;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (context.instantiateForNewVm && context.keyVersion == null) {
                    trigger.next();
                    return;
                }
                if (StringUtils.isBlank(context.providerUuid)) {
                    trigger.fail(operr("missing TPM resource key binding for tpm[uuid:%s], attachKeyProviderToTpm must run before get-secret-on-host",
                            context.tpmUuid));
                    return;
                }
                if (!context.instantiateForNewVm && context.keyVersion == null) {
                    trigger.fail(operr("missing keyVersion for tpm[uuid:%s] before get secret on host", context.tpmUuid));
                    return;
                }
                // NewCreate cloned from an existing TPM may already carry keyVersion; allow it.
                // For non-NewCreate, keyVersion must exist (validated above).
                SecretHostGetMsg innerMsg = new SecretHostGetMsg();
                innerMsg.setHostUuid(spec.getDestHost().getUuid());
                innerMsg.setVmUuid(spec.getVmInventory().getUuid());
                innerMsg.setPurpose("vtpm");
                innerMsg.setKeyVersion(context.keyVersion);
                innerMsg.setUsageInstance(HOST_SECRET_USAGE_INSTANCE_VTPM);
                bus.makeTargetServiceIdByResourceUuid(innerMsg, HostConstant.SERVICE_ID, innerMsg.getHostUuid());
                bus.send(innerMsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            SecretHostGetReply r = reply.castReply();
                            spec.getDevicesSpec().getTpm().setSecretUuid(r.getSecretUuid());
                            trigger.next();
                            return;
                        }

                        ErrorCode errorCode = reply.getError();
                        if (errorCode != null && isVtpmSecretNotFoundOnHost(errorCode)) {
                            trigger.next();
                            return;
                        }

                        trigger.fail(errorCode != null ? errorCode : operr("get secret on host failed"));
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "get-or-create-key-and-dek";

            @Override
            public boolean skip(Map data) {
                return !context.enableKeyProvider;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                GetOrCreateResourceKeyContext keyCtx = new GetOrCreateResourceKeyContext();
                keyCtx.setResourceUuid(context.tpmUuid);
                keyCtx.setResourceType(TpmVO.class.getSimpleName());
                keyCtx.setKeyProviderUuid(context.providerUuid);
                keyCtx.setPurpose("vtpm");

                resourceKeyManager.getOrCreateKey(keyCtx, new ReturnValueCompletion<ResourceKeyResult>(trigger) {
                    @Override
                    public void success(ResourceKeyResult result) {
                        tpmSpec.setResourceKeyCreatedNew(result.isCreatedNewKey());
                        tpmSpec.setResourceKeyProviderUuid(result.getKeyProviderUuid());
                        context.dekBase64 = result.getDekBase64();
                        context.keyVersion = result.getKeyVersion();
                        if (context.keyVersion == null) {
                            trigger.fail(operr("missing keyVersion for tpm[uuid:%s] after getOrCreateKey", context.tpmUuid));
                            return;
                        }
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
            }
        }).then(new NoRollbackFlow() {
            String __name__ = "define-secret-on-host";

            @Override
            public boolean skip(Map data) {
                return !context.enableKeyProvider;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (context.dekBase64 == null) {
                    trigger.fail(operr("missing dekBase64 for tpm[uuid:%s] before define-secret-on-host", context.tpmUuid));
                    return;
                }

                SecretHostDefineMsg innerMsg = new SecretHostDefineMsg();
                innerMsg.setHostUuid(spec.getDestHost().getUuid());
                innerMsg.setVmUuid(spec.getVmInventory().getUuid());
                innerMsg.setDekBase64(context.dekBase64);
                innerMsg.setPurpose("vtpm");
                innerMsg.setKeyVersion(context.keyVersion);
                innerMsg.setUsageInstance(HOST_SECRET_USAGE_INSTANCE_VTPM);
                innerMsg.setDescription("Define secret for VM " + spec.getVmInventory().getUuid());
                bus.makeTargetServiceIdByResourceUuid(innerMsg, HostConstant.SERVICE_ID, innerMsg.getHostUuid());
                bus.send(innerMsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            SecretHostDefineReply r = reply.castReply();
                            spec.getDevicesSpec().getTpm().setSecretUuid(r.getSecretUuid());
                            context.clearSensitiveData();
                            trigger.next();
                        } else {
                            context.clearSensitiveData();
                            trigger.fail(reply.getError());
                        }
                    }
                });
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                context.clearSensitiveData();
                completion.success();
            }
        }).error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                context.clearSensitiveData();
                completion.fail(errCode);
            }
        }).start();
    }

    static class PrepareTpmStateHostFileContext {
        String hostUuid;
        String vmUuid;

        // whether the NvRam is on the same host as before
        boolean sameHost = false;
        VmHostFileVO tpmStateFile;
    }

    static class PrepareTpmResourceContext {
        boolean enableKeyProvider;
        String tpmUuid;
        String backupFileUuid;
        String providerUuid;
        Integer keyVersion;
        String dekBase64;
        boolean instantiateForNewVm;

        void clearSensitiveData() {
            dekBase64 = null;
        }
    }

    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        TpmSpec tpmSpec = spec.getDevicesSpec() == null ? null : spec.getDevicesSpec().getTpm();
        if (tpmSpec == null || !tpmSpec.isResourceKeyCreatedNew()) {
            completion.success();
            return;
        }

        ResourceKeyResult result = new ResourceKeyResult();
        result.setResourceUuid(tpmSpec.getTpmUuid());
        result.setResourceType(TpmVO.class.getSimpleName());
        result.setKeyProviderUuid(tpmSpec.getResourceKeyProviderUuid());
        result.setCreatedNewKey(true);

        resourceKeyManager.rollbackCreatedKey(result, new Completion(completion) {
            @Override
            public void success() {
                clearRollbackInfo(spec);
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("failed to rollback TPM resource key for tpm[uuid:%s]: %s",
                        tpmSpec.getTpmUuid(), errorCode != null ? errorCode.getDetails() : ""));
                clearRollbackInfo(spec);
                completion.success();
            }
        });
    }

    @Override
    public void vmJustBeforeDeleteFromDb(VmInstanceInventory inv) {
        String tpmUuid = Q.New(TpmVO.class)
                .eq(TpmVO_.vmInstanceUuid, inv.getUuid())
                .select(TpmVO_.uuid)
                .findValue();
        if (tpmUuid == null) {
            return;
        }

        // Delete host secrets while TPM row and key version are still resolvable.
        // RemoveTpm may skip or fail (e.g. VM not in Stopped), and callback order may change.
        Integer keyVersion = resourceKeyBackend.findKeyVersionByTpm(tpmUuid);
        Set<String> hostUuids = new HashSet<>();
        List<VmHostFileVO> tpmStateFiles = Q.New(VmHostFileVO.class)
                .eq(VmHostFileVO_.vmInstanceUuid, inv.getUuid())
                .eq(VmHostFileVO_.type, VmHostFileType.TpmState)
                .list();
        for (VmHostFileVO f : tpmStateFiles) {
            if (StringUtils.isNotBlank(f.getHostUuid())) {
                hostUuids.add(f.getHostUuid());
            }
        }
        if (StringUtils.isNotBlank(inv.getHostUuid())) {
            hostUuids.add(inv.getHostUuid());
        }
        if (StringUtils.isNotBlank(inv.getLastHostUuid())) {
            hostUuids.add(inv.getLastHostUuid());
        }
        for (String hostUuid : hostUuids) {
            deleteHostSecretBestEffort(hostUuid, inv.getUuid(), keyVersion, "vm-just-before-delete-from-db");
        }

        RemoveTpmMsg removeMsg = new RemoveTpmMsg();
        removeMsg.setVmInstanceUuid(inv.getUuid());
        removeMsg.setTpmUuid(tpmUuid);
        bus.makeTargetServiceIdByResourceUuid(removeMsg, SERVICE_ID, removeMsg.getTpmUuid());
        MessageReply reply = bus.call(removeMsg);
        if (!reply.isSuccess()) {
            logger.warn(String.format("failed to remove TPM[uuid:%s] of VM[uuid:%s], error: %s",
                    tpmUuid, inv.getUuid(), reply.getError()));
        }
    }

    private void clearRollbackInfo(VmInstanceSpec spec) {
        if (spec.getDevicesSpec() == null || spec.getDevicesSpec().getTpm() == null) {
            return;
        }
        spec.getDevicesSpec().getTpm().setResourceKeyCreatedNew(false);
        spec.getDevicesSpec().getTpm().setResourceKeyProviderUuid(null);
    }

    private String findSourceTpmUuidFromSnapshotTpmBackupFile(String tpmBackupFileUuid) {
        if (StringUtils.isBlank(tpmBackupFileUuid)) {
            return null;
        }
        VmHostBackupFileVO bf = Q.New(VmHostBackupFileVO.class)
                .eq(VmHostBackupFileVO_.uuid, tpmBackupFileUuid)
                .eq(VmHostBackupFileVO_.type, VmHostFileType.TpmState)
                .find();
        if (bf == null || StringUtils.isBlank(bf.getResourceUuid())) {
            return null;
        }
        String sourceVmUuid = Q.New(VolumeSnapshotGroupVO.class)
                .select(VolumeSnapshotGroupVO_.vmInstanceUuid)
                .eq(VolumeSnapshotGroupVO_.uuid, bf.getResourceUuid())
                .findValue();
        if (StringUtils.isBlank(sourceVmUuid)) {
            return null;
        }
        return Q.New(TpmVO.class)
                .eq(TpmVO_.vmInstanceUuid, sourceVmUuid)
                .select(TpmVO_.uuid)
                .findValue();
    }

    @Override
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid, Completion completion) {
        if (inv == null || StringUtils.isBlank(destHostUuid)) {
            completion.success();
            return;
        }
        String srcHostUuid = inv.getHostUuid();
        if (StringUtils.isBlank(srcHostUuid)) {
            completion.success();
            return;
        }
        try {
            VtpmMigratePreAgentContext ctx = new VtpmMigratePreAgentContext(inv.getUuid(), srcHostUuid, destHostUuid);
            ctx.setEnableKeyProvider(!VmGlobalConfig.ALLOWED_TPM_VM_WITHOUT_KMS.value(Boolean.class));
            prepareVtpmSecretOnHostsBeforeMigrate(ctx);
            completion.success();
        } catch (OperationFailureException e) {
            completion.fail(e.getErrorCode());
        }
    }

    private void prepareVtpmSecretOnHostsBeforeMigrate(VtpmMigratePreAgentContext ctx) {
        String tpmUuid = VmTpmManager.findTpmUuidForVmOrNull(ctx.getVmUuid());
        if (StringUtils.isBlank(tpmUuid)) {
            return;
        }
        if (!ctx.isEnableKeyProvider()) {
            return;
        }
        ctx.setTpmUuid(tpmUuid);
        ctx.setProviderUuid(resourceKeyBackend.findKeyProviderUuidByTpm(tpmUuid));
        ctx.setProviderName(resourceKeyBackend.findKeyProviderNameByTpm(tpmUuid));
        if (StringUtils.isBlank(ctx.getProviderUuid()) && StringUtils.isBlank(ctx.getProviderName())) {
            throw new OperationFailureException(operr("missing TPM resource key binding for tpm[uuid:%s] before migrate", tpmUuid));
        }
        ctx.setKeyVersion(resourceKeyBackend.findKeyVersionByTpm(tpmUuid));
        if (ctx.getKeyVersion() == null) {
            throw new OperationFailureException(operr("cannot find keyVersion for tpm[uuid:%s] before migrate", tpmUuid));
        }

        GetOrCreateResourceKeyContext keyCtx = new GetOrCreateResourceKeyContext();
        keyCtx.setResourceUuid(ctx.getTpmUuid());
        keyCtx.setResourceType(TpmVO.class.getSimpleName());
        keyCtx.setKeyProviderUuid(ctx.getProviderUuid());
        keyCtx.setKeyProviderName(ctx.getProviderName());
        keyCtx.setPurpose("vtpm");
        ResourceKeyResult result = resourceKeyManager.getKey(keyCtx);
        if (StringUtils.isBlank(result.getDekBase64())) {
            throw new OperationFailureException(operr("missing DEK for tpm[uuid:%s] after getKey before migrate", ctx.getTpmUuid()));
        }
        ctx.setResourceKeyResult(result);

        SecretHostGetMsg getMsg = new SecretHostGetMsg();
        getMsg.setHostUuid(ctx.getSrcHostUuid());
        getMsg.setVmUuid(ctx.getVmUuid());
        getMsg.setPurpose("vtpm");
        getMsg.setKeyVersion(ctx.getKeyVersion());
        getMsg.setUsageInstance(HOST_SECRET_USAGE_INSTANCE_VTPM);
        bus.makeTargetServiceIdByResourceUuid(getMsg, HostConstant.SERVICE_ID, getMsg.getHostUuid());
        MessageReply getReply = bus.call(getMsg);
        if (!getReply.isSuccess()) {
            throw new OperationFailureException(getReply.getError() != null ? getReply.getError()
                    : operr("get secret on source host failed before migrate"));
        }
        SecretHostGetReply getR = getReply.castReply();
        if (StringUtils.isBlank(getR.getSecretUuid())) {
            throw new OperationFailureException(operr("failed to get source secret uuid before migrate: empty secretUuid"));
        }
        ctx.setSourceSecretUuid(getR.getSecretUuid());

        ResourceKeyResult keyResult = ctx.getResourceKeyResult();
        if (keyResult == null || StringUtils.isBlank(keyResult.getDekBase64())) {
            throw new OperationFailureException(operr("missing DEK for tpm[uuid:%s] before ensure secret on destination", ctx.getTpmUuid()));
        }
        SecretHostDefineMsg defMsg = new SecretHostDefineMsg();
        defMsg.setHostUuid(ctx.getDstHostUuid());
        defMsg.setVmUuid(ctx.getVmUuid());
        defMsg.setDekBase64(keyResult.getDekBase64());
        defMsg.setPurpose("vtpm");
        defMsg.setKeyVersion(ctx.getKeyVersion());
        defMsg.setUsageInstance(HOST_SECRET_USAGE_INSTANCE_VTPM);
        defMsg.setSecretUuid(ctx.getSourceSecretUuid());
        defMsg.setDescription(String.format("Define secret for VM %s before live migration", ctx.getVmUuid()));
        bus.makeTargetServiceIdByResourceUuid(defMsg, HostConstant.SERVICE_ID, defMsg.getHostUuid());
        MessageReply defReply = bus.call(defMsg);
        if (!defReply.isSuccess()) {
            throw new OperationFailureException(defReply.getError());
        }
    }

    @Override
    public void afterMigrateVm(VmInstanceInventory inv, String srcHostUuid, NoErrorCompletion completion) {
        String vmUuid = inv == null ? null : inv.getUuid();
        String destHostUuid = inv == null ? null : inv.getHostUuid();
        if (StringUtils.isBlank(vmUuid) || StringUtils.isBlank(srcHostUuid) || srcHostUuid.equals(destHostUuid)) {
            completion.done();
            return;
        }

        if (!isVmCurrentlyOnExpectedHost(vmUuid, destHostUuid)) {
            completion.done();
            return;
        }

        Integer keyVersion = findTpmKeyVersionByVmUuid(vmUuid);
        deleteHostSecretBestEffort(srcHostUuid, vmUuid, keyVersion, "after-migrate");
        completion.done();
    }

    @Override
    public void vmStateChanged(VmInstanceInventory vm, VmInstanceState oldState, VmInstanceState newState) {
        String vmUuid = vm == null ? null : vm.getUuid();
        if (StringUtils.isBlank(vmUuid)) {
            logger.info(String.format("vmStateChanged skip: vmUuid is blank, oldState=%s, newState=%s", oldState, newState));
            return;
        }

        // Record source host when storage migration starts. In some end-state events (e.g. VolumeMigrating->Stopped),
        // inventory host fields may not carry both src/dst host values reliably.
        if (newState == VmInstanceState.VolumeMigrating) {
            String srcHostUuid = vm.getLastHostUuid();
            if (StringUtils.isBlank(srcHostUuid)) {
                srcHostUuid = Q.New(VmInstanceVO.class)
                        .select(VmInstanceVO_.lastHostUuid)
                        .eq(VmInstanceVO_.uuid, vmUuid)
                        .findValue();
            }
            if (StringUtils.isNotBlank(srcHostUuid)) {
                volumeMigratingSourceHostCache.put(vmUuid, srcHostUuid);
                logger.info(String.format(
                        "vmStateChanged cache volume-migrating src host: vm[uuid:%s], oldState=%s, newState=%s, srcHostUuid=%s",
                        vmUuid, oldState, newState, srcHostUuid));
            } else {
                logger.info(String.format(
                        "vmStateChanged skip cache: source host is blank, vm[uuid:%s], oldState=%s, newState=%s",
                        vmUuid, oldState, newState));
            }
            return;
        }

        if (oldState != VmInstanceState.VolumeMigrating) {
            return;
        }

        String srcHostUuid = volumeMigratingSourceHostCache.remove(vmUuid);
        if (StringUtils.isBlank(srcHostUuid)) {
            logger.info(String.format(
                    "vmStateChanged skip delete: no cached source host, vm[uuid:%s], oldState=%s, newState=%s",
                    vmUuid, oldState, newState));
            return;
        }

        String destHostUuid = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.hostUuid)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .findValue();
        if (StringUtils.isBlank(destHostUuid)) {
            destHostUuid = Q.New(VmInstanceVO.class)
                    .select(VmInstanceVO_.lastHostUuid)
                    .eq(VmInstanceVO_.uuid, vmUuid)
                    .findValue();
        }
        if (StringUtils.isBlank(destHostUuid) || srcHostUuid.equals(destHostUuid)) {
            logger.info(String.format(
                    "vmStateChanged skip delete: invalid host mapping, vm[uuid:%s], oldState=%s, newState=%s, srcHostUuid=%s, destHostUuid=%s",
                    vmUuid, oldState, newState, srcHostUuid, destHostUuid));
            return;
        }

        Integer keyVersion = findTpmKeyVersionByVmUuid(vmUuid);
        logger.info(String.format(
                "vmStateChanged trigger delete: vm[uuid:%s], srcHostUuid=%s, destHostUuid=%s, keyVersion=%s, reason=volume-migrated-host-change",
                vmUuid, srcHostUuid, destHostUuid, keyVersion));
        deleteHostSecretBestEffort(srcHostUuid, vmUuid, keyVersion, "volume-migrated-host-change");
    }

    @Override
    public void vmAfterExpunge(VmInstanceInventory vm) {
        String vmUuid = vm.getUuid();
        Integer keyVersion = findTpmKeyVersionByVmUuid(vmUuid);

        java.util.Set<String> hostUuids = new java.util.HashSet<>();
        if (StringUtils.isNotBlank(vm.getHostUuid())) {
            hostUuids.add(vm.getHostUuid());
        }
        if (StringUtils.isNotBlank(vm.getLastHostUuid())) {
            hostUuids.add(vm.getLastHostUuid());
        }

        if (hostUuids.isEmpty()) {
            return;
        }

        for (String hostUuid : hostUuids) {
            deleteHostSecretBestEffort(hostUuid, vmUuid, keyVersion, "expunge");
        }
    }

    private Integer findTpmKeyVersionByVmUuid(String vmUuid) {
        if (StringUtils.isBlank(vmUuid)) {
            return null;
        }
        String tpmUuid = Q.New(TpmVO.class)
                .eq(TpmVO_.vmInstanceUuid, vmUuid)
                .select(TpmVO_.uuid)
                .findValue();
        return tpmUuid == null ? null : resourceKeyBackend.findKeyVersionByTpm(tpmUuid);
    }

    private boolean isVmCurrentlyOnExpectedHost(String vmUuid, String expectedHostUuid) {
        if (StringUtils.isBlank(vmUuid) || StringUtils.isBlank(expectedHostUuid)) {
            return false;
        }

        String currentHostUuid = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.hostUuid)
                .eq(VmInstanceVO_.uuid, vmUuid)
                .findValue();
        return expectedHostUuid.equals(currentHostUuid);
    }

    private static boolean isVtpmSecretNotFoundOnHost(ErrorCode errorCode) {
        if (SecretHostGetReply.ERROR_CODE_SECRET_NOT_FOUND.equals(errorCode.getCode())) {
            return true;
        }
        String details = errorCode.getDetails();
        return details != null && details.contains(SecretHostGetReply.ERROR_CODE_SECRET_NOT_FOUND);
    }

    private void deleteHostSecretBestEffort(String hostUuid, String vmUuid, Integer keyVersion, String reason) {
        if (StringUtils.isBlank(hostUuid) || StringUtils.isBlank(vmUuid) || keyVersion == null) {
            logger.info(String.format(
                    "skip delete host secret: reason=%s, hostUuid=%s, vmUuid=%s, keyVersion=%s",
                    reason, hostUuid, vmUuid, keyVersion));
            return;
        }

        logger.info(String.format(
                "send delete host secret: reason=%s, hostUuid=%s, vmUuid=%s, keyVersion=%s",
                reason, hostUuid, vmUuid, keyVersion));
        SecretHostDeleteMsg dmsg = new SecretHostDeleteMsg();
        dmsg.setHostUuid(hostUuid);
        dmsg.setVmUuid(vmUuid);
        dmsg.setPurpose("vtpm");
        dmsg.setKeyVersion(keyVersion);
        dmsg.setUsageInstance(HOST_SECRET_USAGE_INSTANCE_VTPM);
        bus.makeTargetServiceIdByResourceUuid(dmsg, HostConstant.SERVICE_ID, hostUuid);
        bus.send(dmsg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    ErrorCode err = reply.getError();
                    String errMsg = err != null && err.getDetails() != null ? err.getDetails() : "unknown error";
                    logger.warn(String.format(
                            "best-effort delete host secret failed on %s for vm[uuid:%s], host[uuid:%s]: %s",
                            reason, vmUuid, hostUuid, errMsg));
                }
            }
        });
    }
}
