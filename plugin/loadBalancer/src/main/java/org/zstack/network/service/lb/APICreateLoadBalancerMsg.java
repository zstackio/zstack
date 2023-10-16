package org.zstack.network.service.lb;

import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;
import org.zstack.network.service.vip.VipVO;
import org.zstack.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by frank on 8/8/2015.
 */
@TagResourceType(LoadBalancerVO.class)
@Action(category = LoadBalancerConstants.ACTION_CATEGORY)
@RestRequest(
        path = "/load-balancers",
        method = HttpMethod.POST,
        responseClass = APICreateLoadBalancerEvent.class,
        parameterName = "params"
)
public class APICreateLoadBalancerMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(required = false, resourceType = VipVO.class, checkAccount = true)
    private String vipUuid;

    @APIParam(required = false, resourceType = VipVO.class, checkAccount = true)
    private String ipv6VipUuid;

    @APINoSee
    private List<String> vipUuids;

    private String type;

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

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public String getIpv6VipUuid() {
        return ipv6VipUuid;
    }

    public void setIpv6VipUuid(String ipv6VipUuid) {
        this.ipv6VipUuid = ipv6VipUuid;
    }

    public List<String> getVipUuids() {
        if (!CollectionUtils.isEmpty(vipUuids)) {
            return vipUuids;
        }

        vipUuids = new ArrayList<>();
        if (!StringUtils.isEmpty(vipUuid)) {
            vipUuids.add(vipUuid);
        }
        if (!StringUtils.isEmpty(ipv6VipUuid)) {
            vipUuids.add(ipv6VipUuid);
        }
        return vipUuids;
    }
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public static APICreateLoadBalancerMsg __example__() {
        APICreateLoadBalancerMsg msg = new APICreateLoadBalancerMsg();

        msg.setName("Test-Lb");
        msg.setVipUuid(uuid());

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateLoadBalancerEvent)rsp).getInventory().getUuid() : "", LoadBalancerVO.class);
    }
}
