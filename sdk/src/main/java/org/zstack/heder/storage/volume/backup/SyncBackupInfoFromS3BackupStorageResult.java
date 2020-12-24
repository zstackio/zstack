package org.zstack.heder.storage.volume.backup;

import org.zstack.sdk.SyncBackupInfoResult;

public class SyncBackupInfoFromS3BackupStorageResult {
    public SyncBackupInfoResult result;
    public void setResult(SyncBackupInfoResult result) {
        this.result = result;
    }
    public SyncBackupInfoResult getResult() {
        return this.result;
    }

}
