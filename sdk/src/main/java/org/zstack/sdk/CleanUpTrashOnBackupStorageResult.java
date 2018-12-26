package org.zstack.sdk;

import org.zstack.sdk.CleanTrashResult;

public class CleanUpTrashOnBackupStorageResult {
    public CleanTrashResult result;
    public void setResult(CleanTrashResult result) {
        this.result = result;
    }
    public CleanTrashResult getResult() {
        return this.result;
    }

}
