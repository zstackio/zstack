package org.zstack.kvm.tpm;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.devices.TpmEncryptedResourceKeyBackend;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostConstant;
import org.zstack.header.message.MessageReply;
import org.zstack.header.keyprovider.EncryptedResourceKeyManager;
import org.zstack.header.keyprovider.EncryptedResourceKeyManager.GetOrCreateResourceKeyContext;
import org.zstack.header.keyprovider.EncryptedResourceKeyManager.ResourceKeyResult;
import org.zstack.header.secret.SecretHostDefineMsg;
import org.zstack.header.tpm.entity.TpmSpec;
import org.zstack.header.tpm.entity.TpmVO;
import org.zstack.header.secret.SecretHostDefineReply;
import org.zstack.header.vm.PreVmInstantiateResourceExtensionPoint;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstantiateResourceException;
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
import java.util.Map;

import static org.zstack.kvm.KVMConstant.*;
import static org.zstack.core.Platform.operr;

public class KvmTpmExtensions implements KVMStartVmExtensionPoint,
        PreVmInstantiateResourceExtensionPoint {
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
        context.providerUuid = resourceKeyBackend.findKeyProviderUuidByTpm(context.tpmUuid);
        context.providerName = resourceKeyBackend.findKeyProviderNameByTpm(context.tpmUuid);

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
        }).then(new NoRollbackFlow() {
            String __name__ = "create-dek";

            @Override
            public boolean skip(Map data) {
                return false;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (StringUtils.isBlank(context.providerUuid) || StringUtils.isBlank(context.providerName)) {
                    trigger.fail(operr("missing TPM resource key binding for tpm[uuid:%s], attachKeyProviderToTpm must run before create-dek", context.tpmUuid));
                    return;
                }

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
                        context.providerName = result.getKeyProviderName();
                        context.dekBase64 = result.getDekBase64();
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
                return context.dekBase64 == null;
            }

            @Override
            public void run(FlowTrigger trigger, Map data) {
                if (StringUtils.isBlank(context.providerName)) {
                    trigger.fail(operr("missing effective key provider name for tpm[uuid:%s] before define-secret-on-host", context.tpmUuid));
                    return;
                }

                SecretHostDefineMsg innerMsg = new SecretHostDefineMsg();
                innerMsg.setHostUuid(spec.getDestHost().getUuid());
                innerMsg.setVmUuid(spec.getVmInventory().getUuid());
                innerMsg.setDekBase64(context.dekBase64);
                innerMsg.setPurpose("vtpm");
                innerMsg.setProviderName(context.providerName);
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
        String tpmUuid;
        String providerUuid;
        String providerName;
        String dekBase64;

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
        // TPM resource key creation requires attachKeyProviderToTpm to create a placeholder ref first.
        result.setRefExistedBeforeCreate(true);

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

    private void clearRollbackInfo(VmInstanceSpec spec) {
        if (spec.getDevicesSpec() == null || spec.getDevicesSpec().getTpm() == null) {
            return;
        }
        spec.getDevicesSpec().getTpm().setResourceKeyCreatedNew(false);
        spec.getDevicesSpec().getTpm().setResourceKeyProviderUuid(null);
    }
}
