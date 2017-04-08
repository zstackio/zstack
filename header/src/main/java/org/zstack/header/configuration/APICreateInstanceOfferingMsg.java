package org.zstack.header.configuration;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

import static org.zstack.header.vm.VmInstanceConstant.USER_VM_TYPE;

@Action(category = ConfigurationConstant.ACTION_CATEGORY)
@TagResourceType(InstanceOfferingVO.class)
@RestRequest(
        path = "/instance-offerings",
        responseClass = APICreateInstanceOfferingEvent.class,
        parameterName = "params",
        method = HttpMethod.POST
)
public class APICreateInstanceOfferingMsg extends APICreateMessage {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(required = false, maxLength = 2048)
    private String description;
    @APIParam(numberRange = {1, 1024})
    private int cpuNum;
    @APINoSee
    private int cpuSpeed;
    @APIParam(numberRange = {1, Long.MAX_VALUE})
    private long memorySize;
    private String allocatorStrategy;
    private int sortKey;
    private String type;

    public APICreateInstanceOfferingMsg() {
    }

    public int getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(int cpuNum) {
        this.cpuNum = cpuNum;
    }

    public int getCpuSpeed() {
        return cpuSpeed;
    }

    public void setCpuSpeed(int cpuSpeed) {
        this.cpuSpeed = cpuSpeed;
    }

    public long getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(long memorySize) {
        this.memorySize = memorySize;
    }

    public String getAllocatorStrategy() {
        return allocatorStrategy;
    }

    public void setAllocatorStrategy(String allocatorStrategy) {
        this.allocatorStrategy = allocatorStrategy;
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

    public int getSortKey() {
        return sortKey;
    }

    public void setSortKey(int sortKey) {
        this.sortKey = sortKey;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
 
    public static APICreateInstanceOfferingMsg __example__() {
        APICreateInstanceOfferingMsg msg = new APICreateInstanceOfferingMsg();
        msg.setName("instanceOffering");
        msg.setType(USER_VM_TYPE);
        msg.setCpuNum(2);
        msg.setMemorySize(2097152);


        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("Creating").resource(((APICreateInstanceOfferingEvent)evt).getInventory().getUuid(),
                            InstanceOfferingVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
                }
            }
        };
    }

}
