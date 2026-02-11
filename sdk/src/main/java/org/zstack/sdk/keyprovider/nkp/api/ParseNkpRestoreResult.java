package org.zstack.sdk.keyprovider.nkp.api;

import org.zstack.sdk.NkpRestoreInfo;

public class ParseNkpRestoreResult {
    public NkpRestoreInfo restoreInfo;
    public void setRestoreInfo(NkpRestoreInfo restoreInfo) {
        this.restoreInfo = restoreInfo;
    }
    public NkpRestoreInfo getRestoreInfo() {
        return this.restoreInfo;
    }

}
