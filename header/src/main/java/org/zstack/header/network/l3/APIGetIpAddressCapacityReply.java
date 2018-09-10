package org.zstack.header.network.l3;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.math.BigInteger;

/**
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetIpAddressCapacityReply extends APIReply {
    private BigInteger totalCapacity;
    private BigInteger availableCapacity;

    public BigInteger getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(BigInteger totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public BigInteger getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(BigInteger availableCapacity) {
        this.availableCapacity = availableCapacity;
    }
 
    public static APIGetIpAddressCapacityReply __example__() {
        APIGetIpAddressCapacityReply reply = new APIGetIpAddressCapacityReply();
        reply.setAvailableCapacity(BigInteger.TEN);
        reply.setTotalCapacity(BigInteger.TEN);
        return reply;
    }

}
