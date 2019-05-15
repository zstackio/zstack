package org.zstack.resourceconfig;

import org.zstack.core.Platform;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.Collections;

@RestResponse(allTo = "inventory")
public class APIUpdateResourceConfigEvent extends APIEvent {
    private ResourceConfigInventory inventory;

    public APIUpdateResourceConfigEvent() {
    }

    public APIUpdateResourceConfigEvent(String apiId) {
        super(apiId);
    }

    public ResourceConfigInventory getInventory() {
        return inventory;
    }

    public void setInventory(ResourceConfigInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateResourceConfigEvent __example__() {
        APIUpdateResourceConfigEvent reply = new APIUpdateResourceConfigEvent();
        ResourceConfigInventory clusterConfig = new ResourceConfigInventory();
        clusterConfig.setCategory("host");
        clusterConfig.setName("cpu.overProvisioning.ratio");
        clusterConfig.setResourceType(ClusterVO.class.getSimpleName());
        clusterConfig.setResourceUuid(Platform.getUuid());
        clusterConfig.setUuid(Platform.getUuid());
        clusterConfig.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        clusterConfig.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        clusterConfig.setValue("10");
        reply.inventory = clusterConfig;
        return reply;
    }
}
