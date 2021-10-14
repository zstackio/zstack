package org.zstack.header.host;

import org.zstack.header.message.APIReply;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.RestResponse;


import java.util.*;

@RestResponse(allTo = "inventories")
public class APIQueryHostNUMATopologyReply extends MessageReply {
    private HostNumaInventory inventories;

    public void setInventories(HostNumaInventory inventories) {
        this.inventories = inventories;
    }

    public HostNumaInventory getInventories() {
        return inventories;
    }
}
