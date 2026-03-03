package org.zstack.kvm.efi;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.legacy.ComputeLegacyGlobalProperty;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacadeImpl;
import org.zstack.core.db.Q;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmCanonicalEvents;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.header.vm.additions.VmHostFileVO;
import org.zstack.header.vm.additions.VmHostFileVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.List;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.findOneOrNull;

public class KvmSecureBootManager implements Component {
    private static final CLogger logger = Utils.getLogger(KvmSecureBootManager.class);

    @Autowired
    private EventFacadeImpl eventFacade;
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
}
