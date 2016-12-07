package org.zstack.header.identity;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

/**
 * Created by frank on 7/14/2015.
 */
@RestResponse(allTo = "inventories")
public class APIQueryQuotaReply extends APIQueryReply {
    private List<QuotaInventory> inventories;

    public List<QuotaInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<QuotaInventory> inventories) {
        this.inventories = inventories;
    }
}
