package org.zstack.resourceconfig;

import org.zstack.core.Platform;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

/**
 * Created by MaJin on 2019/2/23.
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetResourceConfigReply extends APIReply {
    private String value;
    private List<ResourceConfigInventory> effectiveConfigs;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public List<ResourceConfigInventory> getEffectiveConfigs() {
        return effectiveConfigs;
    }

    public void setEffectiveConfigs(List<ResourceConfigInventory> effectiveConfigs) {
        this.effectiveConfigs = effectiveConfigs;
    }

    public static APIGetResourceConfigReply __example__ () {
        APIGetResourceConfigReply reply = new APIGetResourceConfigReply();
        reply.value = "5";
        ResourceConfigInventory hostConfig = new ResourceConfigInventory();
        hostConfig.setCategory("host");
        hostConfig.setName("cpu.overProvisioning.ratio");
        hostConfig.setResourceType(HostVO.class.getSimpleName());
        hostConfig.setResourceUuid(Platform.getUuid());
        hostConfig.setUuid(Platform.getUuid());
        hostConfig.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        hostConfig.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        hostConfig.setValue("5");

        ResourceConfigInventory clusterConfig = new ResourceConfigInventory();
        clusterConfig.setCategory("host");
        clusterConfig.setName("cpu.overProvisioning.ratio");
        clusterConfig.setResourceType(ClusterVO.class.getSimpleName());
        clusterConfig.setResourceUuid(Platform.getUuid());
        clusterConfig.setUuid(Platform.getUuid());
        clusterConfig.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        clusterConfig.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        clusterConfig.setValue("10");

        reply.effectiveConfigs = Arrays.asList(hostConfig, clusterConfig);
        return reply;
    }

}
