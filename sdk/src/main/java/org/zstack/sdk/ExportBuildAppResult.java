package org.zstack.sdk;

import org.zstack.sdk.BuildAppExportHistoryInventory;

public class ExportBuildAppResult {
    public BuildAppExportHistoryInventory inventory;
    public void setInventory(BuildAppExportHistoryInventory inventory) {
        this.inventory = inventory;
    }
    public BuildAppExportHistoryInventory getInventory() {
        return this.inventory;
    }

}
