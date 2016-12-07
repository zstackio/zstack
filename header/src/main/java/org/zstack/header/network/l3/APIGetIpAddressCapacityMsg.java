package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.zone.ZoneVO;

import java.util.List;

/**
 */
@Action(category = L3NetworkConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/ip-capacity",
        method = HttpMethod.GET,
        parameterName = "params",
        responseClass = APIGetIpAddressCapacityReply.class
)
public class APIGetIpAddressCapacityMsg extends APISyncCallMessage {
    @APIParam(required = false, resourceType = ZoneVO.class)
    private List<String> zoneUuids;
    @APIParam(required = false, resourceType = L3NetworkVO.class, checkAccount = true)
    private List<String> l3NetworkUuids;
    @APIParam(required = false, resourceType = IpRangeVO.class, checkAccount = true)
    private List<String> ipRangeUuids;
    private boolean all;

    public List<String> getZoneUuids() {
        return zoneUuids;
    }

    public void setZoneUuids(List<String> zoneUuids) {
        this.zoneUuids = zoneUuids;
    }

    public List<String> getL3NetworkUuids() {
        return l3NetworkUuids;
    }

    public void setL3NetworkUuids(List<String> l3NetworkUuids) {
        this.l3NetworkUuids = l3NetworkUuids;
    }

    public List<String> getIpRangeUuids() {
        return ipRangeUuids;
    }

    public void setIpRangeUuids(List<String> ipRangeUuids) {
        this.ipRangeUuids = ipRangeUuids;
    }

    public boolean isAll() {
        return all;
    }

    public void setAll(boolean all) {
        this.all = all;
    }
}
