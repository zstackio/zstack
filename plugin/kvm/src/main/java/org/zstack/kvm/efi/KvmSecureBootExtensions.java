package org.zstack.kvm.efi;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.compute.vm.devices.VmTpmManager;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.header.core.Completion;
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
import org.zstack.kvm.KVMGlobalConfig;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMStartVmExtensionPoint;
import org.zstack.kvm.VolumeTO;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.Map;
import java.util.Objects;

import static org.zstack.core.Platform.operr;
import static org.zstack.kvm.KVMConstant.EDK_VERSION_NONE;

public class KvmSecureBootExtensions implements KVMStartVmExtensionPoint,
        PreVmInstantiateResourceExtensionPoint {
    private static final CLogger logger = Utils.getLogger(KvmSecureBootExtensions.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private ResourceConfigFacade resourceConfigFacade;

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
