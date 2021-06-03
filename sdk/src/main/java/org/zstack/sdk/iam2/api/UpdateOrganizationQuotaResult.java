package org.zstack.sdk.iam2.api;

import org.zstack.sdk.QuotaInventory;

public class UpdateOrganizationQuotaResult {
    public QuotaInventory inventory;
    public void setInventory(QuotaInventory inventory) {
        this.inventory = inventory;
    }
    public QuotaInventory getInventory() {
        return this.inventory;
    }

}
