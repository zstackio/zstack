package org.zstack.header.network.l3;

import org.zstack.header.message.APIReply;

/**
 * Created by frank on 1/21/2016.
 */
public class APICheckIpAvailabilityReply extends APIReply {
    private boolean available;

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
