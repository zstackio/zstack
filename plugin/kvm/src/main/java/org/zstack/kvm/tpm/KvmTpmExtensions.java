package org.zstack.kvm.tpm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
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

import static org.zstack.kvm.KVMConstant.*;

public class KvmTpmExtensions implements KVMStartVmExtensionPoint,
        PreVmInstantiateResourceExtensionPoint {
    private static final CLogger logger = Utils.getLogger(KvmTpmExtensions.class);

    @Autowired
    private KvmSecureBootExtensions secureBootExtensions;
    @Autowired
    private DatabaseFacade databaseFacade;

    private final Object hostFileLock = new Object();

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        final VmDevicesSpec devicesSpec = spec.getDevicesSpec();
        if (devicesSpec == null || devicesSpec.getTpm() == null || !devicesSpec.getTpm().isEnable()) {
            return;
        }

        TpmTO tpm = new TpmTO();
        tpm.setKeyProviderUuid(devicesSpec.getTpm().getKeyProviderUuid());
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
        // do-nothing
    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {
        // do-nothing
    }

    @Override
    public void preBeforeInstantiateVmResource(VmInstanceSpec spec) throws VmInstantiateResourceException {
        // do-nothing
    }

    @Override
    public void preInstantiateVmResource(VmInstanceSpec spec, Completion completion) {
        prepareTpmStateHostFileOnHost(spec, completion);
    }

    static class PrepareTpmStateHostFileContext {
        String hostUuid;
        String vmUuid;

        // whether the NvRam is on the same host as before
        boolean sameHost = false;
        VmHostFileVO tpmStateFile;
    }

    private void prepareTpmStateHostFileOnHost(VmInstanceSpec spec, Completion completion) {
        final VmDevicesSpec devicesSpec = spec.getDevicesSpec();
        if (devicesSpec == null || devicesSpec.getTpm() == null || !devicesSpec.getTpm().isEnable()) {
            completion.success();
            return;
        }

        PrepareHostFileContext context = new PrepareHostFileContext();
        context.hostUuid = spec.getDestHost().getUuid();
        context.vmUuid = spec.getVmInventory().getUuid();
        context.type = VmHostFileType.TpmState;
        secureBootExtensions.prepareHostFileOnHost(context, completion);
    }


    @Override
    public void preReleaseVmResource(VmInstanceSpec spec, Completion completion) {
        completion.success();
    }
}
