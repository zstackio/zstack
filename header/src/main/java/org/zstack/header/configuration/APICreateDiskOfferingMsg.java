package org.zstack.header.configuration;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@Action(category = ConfigurationConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/disk-offerings",
        method = HttpMethod.POST,
        responseClass = APICreateDiskOfferingEvent.class,
        parameterName = "params"
)
public class APICreateDiskOfferingMsg extends APICreateMessage {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(required = false, maxLength = 2048)
    private String description;
    @APIParam(numberRange = {1, Long.MAX_VALUE})
    private long diskSize;
    private int sortKey;
    private String allocationStrategy;
    @APIParam(required = false, validValues = {"zstack"})
    private String type;

    public APICreateDiskOfferingMsg() {
        super();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getDiskSize() {
        return diskSize;
    }

    public void setDiskSize(long diskSize) {
        this.diskSize = diskSize;
    }

    public int getSortKey() {
        return sortKey;
    }

    public void setSortKey(int sortKey) {
        this.sortKey = sortKey;
    }

    public String getAllocationStrategy() {
        return allocationStrategy;
    }

    public void setAllocationStrategy(String allocatorStrategy) {
        this.allocationStrategy = allocatorStrategy;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
