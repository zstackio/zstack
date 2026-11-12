package org.zstack.header.storage.primary;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.volume.VolumeStats;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.utils.DebugUtils;

/**
 * Created by mingjian.deng on 2017/11/3.
 */
public abstract class BackupStorageMediator {
    public BackupStorageInventory backupStorage;
    public MediatorParam param;

    public void checkParam() {
        DebugUtils.Assert(backupStorage != null, "backupStorage cannot be null");
        DebugUtils.Assert(param != null, "param cannot be null");
    }

    abstract public void download(ReturnValueCompletion<String> completion);

    public void downloadVolume(ReturnValueCompletion<VolumeStats> completion) {
        download(new ReturnValueCompletion<String>(completion) {
            @Override
            public void success(String installPath) {
                completion.success(new VolumeStats(installPath, null));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    abstract public void upload(ReturnValueCompletion<String> completion);

    abstract public boolean deleteWhenRollbackDownload();
}
