package org.zstack.header.network.l3;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetIpAddressCapacityReply extends APIReply {
    private long totalCapacity;
    private long availableCapacity;

    public long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }
 
    public static APIGetIpAddressCapacityReply __example__() {
        APIGetIpAddressCapacityReply reply = new APIGetIpAddressCapacityReply();
        reply.setAvailableCapacity(229L);
        reply.setTotalCapacity(245L);
        return reply;
    }

}
