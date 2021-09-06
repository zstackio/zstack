package org.zstack.storage.primary.local;

import org.zstack.core.db.Q;
import org.zstack.core.gc.*;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostCanonicalEvents;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.storage.primary.PrimaryStorageCanonicalEvent;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.kvm.KvmCommandFailureChecker;
import org.zstack.kvm.KvmCommandSender;
import org.zstack.kvm.KvmResponseWrapper;

import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.operr;

/**
 * Created by xing5 on 2017/3/5.
 */
public class LocalStorageDeleteBitsGC extends EventBasedGarbageCollector {
    @GC
    public String primaryStorageUuid;
    @GC
    public String hostUuid;
    @GC
    public String installPath;
    @GC
    public boolean isDir;

    @Override
    protected void triggerNow(GCCompletion completion) {
        if (!dbf.isExist(primaryStorageUuid, PrimaryStorageVO.class)) {
            completion.cancel();
            return;
        }

        if (!dbf.isExist(hostUuid, HostVO.class)) {
            completion.cancel();
            return;
        }

        LocalStorageKvmBackend.DeleteBitsCmd cmd = new LocalStorageKvmBackend.DeleteBitsCmd();
        cmd.setPath(installPath);
        cmd.setHostUuid(hostUuid);
        cmd.storagePath = Q.New(PrimaryStorageVO.class).
                eq(PrimaryStorageVO_.uuid, primaryStorageUuid).
                select(PrimaryStorageVO_.url).
                findValue();

        String path = isDir ? LocalStorageKvmBackend.DELETE_DIR_PATH : LocalStorageKvmBackend.DELETE_BITS_PATH;

        new KvmCommandSender(hostUuid).send(cmd, path,
                new KvmCommandFailureChecker() {
                    @Override
                    public ErrorCode getError(KvmResponseWrapper wrapper) {
                        LocalStorageKvmBackend.DeleteBitsRsp rsp = wrapper.getResponse(LocalStorageKvmBackend.DeleteBitsRsp.class);
                        return rsp.isSuccess() ? null : operr("operation error, because:%s", rsp.getError());
                    }
                },

                new ReturnValueCompletion<KvmResponseWrapper>(completion) {
                    @Override
                    public void success(KvmResponseWrapper ret) {
                        completion.success();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                }
        );
    }

    @Override
    protected void setup() {
        onEvent(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH, ((tokens, data) -> {
            HostCanonicalEvents.HostStatusChangedData d = (HostCanonicalEvents.HostStatusChangedData) data;
            return d.getHostUuid().equals(hostUuid) && d.getNewStatus().equals(HostStatus.Connected.toString());
        }));

        onEvent(HostCanonicalEvents.HOST_DELETED_PATH, ((tokens, data) -> {
            HostCanonicalEvents.HostDeletedData d = (HostCanonicalEvents.HostDeletedData) data;
            return d.getHostUuid().equals(hostUuid);
        }));

        onEvent(PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_DELETED_PATH, ((tokens, data) -> {
            PrimaryStorageCanonicalEvent.PrimaryStorageDeletedData d = (PrimaryStorageCanonicalEvent.PrimaryStorageDeletedData) data;
            return d.getPrimaryStorageUuid().equals(primaryStorageUuid);
        }));
    }

    void deduplicateSubmit() {
        boolean existGc = false;

        GarbageCollectorVO gcVo = Q.New(GarbageCollectorVO.class).eq(GarbageCollectorVO_.name, NAME).notEq(GarbageCollectorVO_.status, GCStatus.Done).find();

        if (gcVo != null) {
            existGc = true;
        }

        if (existGc) {
            return;
        }

        submit();
    }
}
