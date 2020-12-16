package org.zstack.heder.storage.volume.backup;

import org.zstack.sdk.GetBackupInfoResult;

public class GetBackupInfoFromS3BackupStorageResult {
    public GetBackupInfoResult result;
    public void setResult(GetBackupInfoResult result) {
        this.result = result;
    }
    public GetBackupInfoResult getResult() {
        return this.result;
    }

}
