package org.zstack.header.network.l3;

import org.zstack.header.message.APIReply;
import org.zstack.header.network.l3.datatypes.IpCapacityData;
import org.zstack.header.rest.RestResponse;

import java.util.ArrayList;
import java.util.List;


/**
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetIpAddressCapacityReply extends APIReply {
    private long totalCapacity;
    private long availableCapacity;
    private long usedIpAddressNumber;
    private List<IpCapacityData> capacityData;
    private String resourceType;

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

    public List<IpCapacityData> getCapacityData() {
        return capacityData;
    }

    public void setCapacityData(List<IpCapacityData> capacityData) {
        this.capacityData = capacityData;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public static APIGetIpAddressCapacityReply __example__() {
        APIGetIpAddressCapacityReply reply = new APIGetIpAddressCapacityReply();
        reply.setAvailableCapacity(15L);
        reply.setTotalCapacity(20L);
        reply.setUsedIpAddressNumber(5L);
        reply.setResourceType("L3NetworkVO");
        List<IpCapacityData> capacityData = new ArrayList<>();
        reply.setCapacityData(capacityData);
        IpCapacityData data = new IpCapacityData();
        capacityData.add(data);
        data.setAvailableCapacity(15L);
        data.setTotalCapacity(20L);
        data.setUsedIpAddressNumber(5L);
        data.setResourceUuid(uuid());
        return reply;
    }

}
