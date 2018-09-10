package org.zstack.header.network.l3;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.math.BigInteger;

/**
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetIpAddressCapacityReply extends APIReply {
    private long totalCapacity;
    private long availableCapacity;
    private long usedIpAddressNumber;

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

    public long getUsedIpAddressNumber() {
        return usedIpAddressNumber;
    }

    public void setUsedIpAddressNumber(long usedIpAddressNumber) {
        this.usedIpAddressNumber = usedIpAddressNumber;
    }

    public static APIGetIpAddressCapacityReply __example__() {
        APIGetIpAddressCapacityReply reply = new APIGetIpAddressCapacityReply();
        reply.setAvailableCapacity(10L);
        reply.setTotalCapacity(10L);
        reply.setUsedIpAddressNumber(10L);
        return reply;
    }

}
