package org.zstack.network.service.lb;

import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.network.service.vip.VipVO;

import java.util.List;

/**
 * Created by frank on 8/8/2015.
 */
public class APICreateLoadBalancerMsg extends APICreateMessage {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(resourceType = VipVO.class, required = false, nonempty = true)
    private List<String> vipUuids;

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

    public List<String> getVipUuids() {
        return vipUuids;
    }

    public void setVipUuids(List<String> vipUuids) {
        this.vipUuids = vipUuids;
    }
}
