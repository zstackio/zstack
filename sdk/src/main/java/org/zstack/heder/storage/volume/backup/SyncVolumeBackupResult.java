package org.zstack.heder.storage.volume.backup;

import org.zstack.sdk.SyncBackupResult;

/**
 * @Deprecated
 * use {@link org.zstack.sdk.storage.volumebackup.SyncVolumeBackupResult}.
 * this class will removed in zsv_5.4.0
 */
@Deprecated
public class SyncVolumeBackupResult {
    public SyncBackupResult result;
    public void setResult(SyncBackupResult result) {
        this.result = result;
    }
    public SyncBackupResult getResult() {
        return this.result;
    }

}
