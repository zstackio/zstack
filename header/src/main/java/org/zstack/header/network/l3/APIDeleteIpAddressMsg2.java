package org.zstack.header.network.l3;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.*;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

import java.util.List;

@TagResourceType(L3NetworkVO.class)
@Action(category = L3NetworkConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/l3-networks/{l3NetworkUuid}/ip-address",
        method = HttpMethod.PUT,
        responseClass = APIDeleteIpAddressEvent.class
)
public class APIDeleteIpAddressMsg {
    /**
     * @desc l3Network uuid
     */
    @APIParam(resourceType = L3NetworkVO.class, checkAccount = true, operationTarget = true)
    private String l3NetworkUuid;

    @APIParam(resourceType = UsedIpVO.class, checkAccount = true, operationTarget = true)
    private List<String> usedIpUuids;

    @Override
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public List<String> getUsedIpUuids() {
        return usedIpUuids;
    }

    public void setUsedIpUuids(List<String> usedIpUuids) {
        this.usedIpUuids = usedIpUuids;
    }
}
