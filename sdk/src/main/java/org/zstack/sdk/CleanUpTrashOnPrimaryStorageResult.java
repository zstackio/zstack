package org.zstack.sdk;

import org.zstack.sdk.CleanTrashResult;

public class CleanUpTrashOnPrimaryStorageResult {
    public CleanTrashResult result;
    public void setResult(CleanTrashResult result) {
        this.result = result;
    }
    public CleanTrashResult getResult() {
        return this.result;
    }

}
