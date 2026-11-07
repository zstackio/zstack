package org.zstack.compute.vm.devices;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.db.TransactionalCallback.Operation;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.image.ImageBootMode;
import org.zstack.header.message.MessageReply;
import org.zstack.header.tpm.TpmConstants;
import org.zstack.header.tpm.entity.TpmVO;
import org.zstack.header.tpm.entity.TpmVO_;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.additions.ResetVmTpmMsg;
import org.zstack.header.vm.additions.VmHostBackupFileVO;
import org.zstack.header.vm.additions.VmHostBackupFileVO_;
import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.header.vm.additions.VmHostFileVO;
import org.zstack.header.vm.additions.VmHostFileVO_;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeVO;
import org.zstack.identity.AccountManager;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.compute.vm.VmGlobalConfig.ENABLE_UEFI_SECURE_BOOT;
import static org.zstack.core.Platform.err;
import static org.zstack.header.tpm.TpmConstants.SUPPORT_VM_STATES_FOR_TPM_OPERATION;
import static org.zstack.header.tpm.TpmErrors.TPM_ALREADY_EXISTS;
import static org.zstack.header.tpm.TpmErrors.VM_STATE_ERROR;
import static org.zstack.header.vm.additions.VmHostFileType.NvRam;
import static org.zstack.header.vm.additions.VmHostFileType.TpmState;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_KVM_10163;

public class VmTpmManager {
    private static final CLogger logger = Utils.getLogger(VmTpmManager.class);

    @Autowired
    private DatabaseFacade databaseFacade;
    @Autowired
    private ResourceConfigFacade resourceConfigFacade;
    @Autowired
    private AccountManager accountManager;
    @Autowired
    private CloudBus bus;

    public TpmVO persistTpmVO(String tpmUuid, String vmUuid) {
        TpmVO tpm = newTpmVO(tpmUuid, vmUuid);
        databaseFacade.persistAndRefresh(tpm);

        logger.debug("Persisted TpmVO for VM " + vmUuid + " with uuid=" + tpm.getUuid());
        return tpm;
    }

    public TpmVO persistTpmForStoppedVm(String tpmUuid, String vmUuid) {
        String rootUuid = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid)
                .select(VmInstanceVO_.rootVolumeUuid).findValue();
        if (rootUuid == null) {
            throw new OperationFailureException(err(ORG_ZSTACK_KVM_10163, VM_STATE_ERROR,
                    "VM[uuid:%s] has no root volume for adding TPM", vmUuid));
        }

        return new SQLBatchWithReturn<TpmVO>() {
            @Override
            protected TpmVO scripts() {
                EntityManager em = databaseFacade.getEntityManager();
                // Cold migration's status UPDATE competes for this same row. Keep the
                // lock only until the TPM insert commits, before any asynchronous RPC.
                VolumeVO root = em.find(VolumeVO.class, rootUuid, LockModeType.PESSIMISTIC_WRITE);
                if (root != null) {
                    em.refresh(root, LockModeType.PESSIMISTIC_WRITE);
                }
                VmInstanceVO vm = em.find(VmInstanceVO.class, vmUuid);
                if (vm != null) {
                    em.refresh(vm);
                }
                if (root == null || vm == null || !Objects.equals(rootUuid, vm.getRootVolumeUuid())
                        || root.getStatus() != VolumeStatus.Ready
                        || !SUPPORT_VM_STATES_FOR_TPM_OPERATION.contains(vm.getState())) {
                    throw new OperationFailureException(err(ORG_ZSTACK_KVM_10163, VM_STATE_ERROR,
                            "VM[uuid:%s] must be stopped with its root volume ready for adding TPM", vmUuid));
                }
                if (!em.createQuery("select t.uuid from TpmVO t where t.vmInstanceUuid = :vmUuid", String.class)
                        .setParameter("vmUuid", vmUuid).getResultList().isEmpty()) {
                    throw new OperationFailureException(err(ORG_ZSTACK_KVM_10163, TPM_ALREADY_EXISTS,
                            "tpm device already exists"));
                }

                // persistAndRefresh starts REQUIRES_NEW; use this transaction's
                // EntityManager so insertion and the root-volume lock commit together.
                databaseFacade.entityForTranscationCallback(Operation.PERSIST, TpmVO.class);
                return reload(persist(newTpmVO(tpmUuid, vmUuid)));
            }
        }.execute();
    }

    private TpmVO newTpmVO(String tpmUuid, String vmUuid) {
        if (tpmUuid == null) {
            tpmUuid = Platform.getUuid();
        }
        TpmVO tpm = new TpmVO();
        tpm.setUuid(tpmUuid);
        tpm.setResourceName("TPM-for-VM-" + vmUuid);
        tpm.setVmInstanceUuid(vmUuid);
        tpm.setAccountUuid(accountManager.getOwnerAccountUuidOfResource(vmUuid));
        return tpm;
    }

    public void deleteTpmVO(String tpmUuid) {
        databaseFacade.removeByPrimaryKey(tpmUuid, TpmVO.class);
    }

    public static String findTpmUuidForVmOrNull(String vmInstanceUuid) {
        return Q.New(TpmVO.class)
                .eq(TpmVO_.vmInstanceUuid, vmInstanceUuid)
                .select(TpmVO_.uuid)
                .findValue();
    }

    /**
     * @param bootMode boot mode, null is Legacy
     */
    public static boolean isUefiBootMode(String bootMode) {
        return Objects.equals(bootMode, ImageBootMode.UEFI.toString())
                || Objects.equals(bootMode, ImageBootMode.UEFI_WITH_CSM.toString());
    }

    public boolean needRegisterNvRam(String vmUuid) {
        return needRegister(NvRam, vmUuid);
    }

    public Set<VmHostFileType> vmHostFileTypeNeedRegisterForVm(String vmUuid) {
        String bootMode = VmSystemTags.BOOT_MODE.getTokenByResourceUuid(vmUuid, VmSystemTags.BOOT_MODE_TOKEN);
        if (!isUefiBootMode(bootMode)) {
            return Collections.emptySet();
        }

        boolean hasTpm = Q.New(TpmVO.class)
                .eq(TpmVO_.vmInstanceUuid, vmUuid)
                .isExists();
        if (hasTpm) {
            return new HashSet<>(list(NvRam, TpmState));
        }
        ResourceConfig resourceConfig = resourceConfigFacade.getResourceConfig(ENABLE_UEFI_SECURE_BOOT.getIdentity());
        return resourceConfig.getResourceConfigValue(vmUuid, Boolean.class) == Boolean.TRUE ?
                new HashSet<>(list(NvRam)) : Collections.emptySet();
    }

    public boolean needRegister(VmHostFileType type, String vmUuid) {
        return vmHostFileTypeNeedRegisterForVm(vmUuid).contains(type);
    }

    public void resetVmHostFilesIfNeeded(String vmUuid, Completion completion) {
        boolean needReset = Q.New(TpmVO.class).eq(TpmVO_.vmInstanceUuid, vmUuid).isExists()
                || Q.New(VmHostFileVO.class).eq(VmHostFileVO_.vmInstanceUuid, vmUuid).isExists()
                || Q.New(VmHostBackupFileVO.class).eq(VmHostBackupFileVO_.resourceUuid, vmUuid).isExists();
        if (!needReset) {
            completion.success();
            return;
        }

        ResetVmTpmMsg msg = new ResetVmTpmMsg();
        msg.setVmInstanceUuid(vmUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, TpmConstants.SERVICE_ID, vmUuid);
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    public Set<VmHostFileType> vmHostFileTypeToSaveForVm(String vmUuid) {
        Set<VmHostFileType> types = new HashSet<>(vmHostFileTypeNeedRegisterForVm(vmUuid));
        if (findTpmUuidForVmOrNull(vmUuid) != null) {
            types.add(TpmState);
        }
        return types;
    }

    public List<VmHostFileVO> filterHostFilesNeedRegister(String vmUuid, List<VmHostFileVO> hostFiles) {
        if (hostFiles.isEmpty()) {
            return hostFiles;
        }

        Set<VmHostFileType> types = vmHostFileTypeToSaveForVm(vmUuid);
        return hostFiles.stream()
                .filter(it -> types.contains(it.getType()))
                .collect(Collectors.toList());
    }
}
