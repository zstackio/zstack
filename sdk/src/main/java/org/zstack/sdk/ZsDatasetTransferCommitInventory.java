package org.zstack.sdk;

import org.zstack.sdk.ZsDatasetTransferInventory;
import org.zstack.sdk.ZsDatasetSourceInventory;
import org.zstack.sdk.ZsDatasetImageInventory;
import org.zstack.sdk.ZsDatasetImageImportInventory;

public class ZsDatasetTransferCommitInventory  {

    public ZsDatasetTransferInventory transfer;
    public void setTransfer(ZsDatasetTransferInventory transfer) {
        this.transfer = transfer;
    }
    public ZsDatasetTransferInventory getTransfer() {
        return this.transfer;
    }

    public ZsDatasetSourceInventory source;
    public void setSource(ZsDatasetSourceInventory source) {
        this.source = source;
    }
    public ZsDatasetSourceInventory getSource() {
        return this.source;
    }

    public ZsDatasetImageInventory image;
    public void setImage(ZsDatasetImageInventory image) {
        this.image = image;
    }
    public ZsDatasetImageInventory getImage() {
        return this.image;
    }

    public ZsDatasetImageImportInventory importResult;
    public void setImportResult(ZsDatasetImageImportInventory importResult) {
        this.importResult = importResult;
    }
    public ZsDatasetImageImportInventory getImportResult() {
        return this.importResult;
    }

}
