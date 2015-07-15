package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 7/14/2015.
 */
public class APIUpdateQuotaEvent extends APIEvent {
    private QuotaInventory inventory;

    public APIUpdateQuotaEvent() {
    }

    public APIUpdateQuotaEvent(String apiId) {
        super(apiId);
    }

    public QuotaInventory getInventory() {
        return inventory;
    }

    public void setInventory(QuotaInventory inventory) {
        this.inventory = inventory;
    }
}
