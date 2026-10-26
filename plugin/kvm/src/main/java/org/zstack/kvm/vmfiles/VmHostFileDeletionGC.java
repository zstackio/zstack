package org.zstack.kvm.vmfiles;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.Q;
import org.zstack.core.gc.EventBasedGarbageCollector;
import org.zstack.core.gc.GC;
import org.zstack.core.gc.GCCompletion;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostCanonicalEvents;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.additions.VmHostFileType;
import org.zstack.header.vm.additions.VmHostFileVO;
import org.zstack.header.vm.additions.VmHostFileVO_;
import org.zstack.kvm.efi.KvmSecureBootManager;

import java.util.EnumSet;
import java.util.Set;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

public class VmHostFileDeletionGC extends EventBasedGarbageCollector {
    private static final Set<VmInstanceState> VM_STATES_ALLOW_DELETION = EnumSet.of(
            VmInstanceState.Destroying, VmInstanceState.Destroyed, VmInstanceState.Expunging);

    @GC
    public String hostUuid;
    @GC
    public String vmInstanceUuid;
    @GC
    public VmHostFileType type;
    @GC
    public String path;

    @Autowired
    private KvmSecureBootManager secureBootManager;

    public static void submit(VmHostFileVO file) {
        VmHostFileDeletionGC gc = new VmHostFileDeletionGC();
        gc.hostUuid = file.getHostUuid();
        gc.vmInstanceUuid = file.getVmInstanceUuid();
        gc.type = file.getType();
        gc.path = file.getPath();
        gc.NAME = String.format("gc-vm-host-file-%s-on-host-%s", file.getPath(), file.getHostUuid());
        gc.deduplicateSubmit();
    }

    @Override
    protected void setup() {
        onEvent(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH, (tokens, data) -> {
            HostCanonicalEvents.HostStatusChangedData d = (HostCanonicalEvents.HostStatusChangedData) data;
            return d.getHostUuid().equals(hostUuid) && d.getNewStatus().equals(HostStatus.Connected.toString());
        });

        onEvent(HostCanonicalEvents.HOST_DELETED_PATH, (tokens, data) -> {
            HostCanonicalEvents.HostDeletedData d = (HostCanonicalEvents.HostDeletedData) data;
            return d.getHostUuid().equals(hostUuid);
        });
    }

    @Override
    protected void triggerNow(GCCompletion completion) {
        HostStatus status = Q.New(HostVO.class).select(HostVO_.status)
                .eq(HostVO_.uuid, hostUuid).findValue();
        if (status == null) {
            completion.cancel();
            return;
        }

        if (status != HostStatus.Connected) {
            completion.fail(operr(ORG_ZSTACK_KVM_10163, "the host[uuid:%s] is not connected", hostUuid));
            return;
        }

        boolean fileInUse = Q.New(VmHostFileVO.class)
                .eq(VmHostFileVO_.hostUuid, hostUuid)
                .eq(VmHostFileVO_.path, path)
                .isExists();
        VmInstanceState vmState = Q.New(VmInstanceVO.class).select(VmInstanceVO_.state)
                .eq(VmInstanceVO_.uuid, vmInstanceUuid).findValue();
        boolean vmAlive = vmState != null && !VM_STATES_ALLOW_DELETION.contains(vmState);
        if (fileInUse || vmAlive) {
            completion.cancel();
            return;
        }

        secureBootManager.deleteVmHostFileOnHost(hostUuid, type, path, new Completion(completion) {
            @Override
            public void success() {
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }
}
