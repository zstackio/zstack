package org.zstack.sdk;

import org.zstack.sdk.CCSCertificateInventory;

public class UpdateCCSCertificateAccountStateResult {
    public CCSCertificateInventory inventory;
    public void setInventory(CCSCertificateInventory inventory) {
        this.inventory = inventory;
    }
    public CCSCertificateInventory getInventory() {
        return this.inventory;
    }

}
