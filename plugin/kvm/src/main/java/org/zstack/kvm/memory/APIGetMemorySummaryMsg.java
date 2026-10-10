package org.zstack.kvm.memory;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.*;
import org.zstack.header.rest.RestRequest;

@Action(category = org.zstack.header.host.HostConstant.ACTION_CATEGORY, adminOnly = true, names = {"read"})
@RestRequest(path = "/memory-summary", method = HttpMethod.GET, responseClass = APIGetMemorySummaryReply.class)
public class APIGetMemorySummaryMsg extends APISyncCallMessage {
    @APIParam(required = false, resourceType = org.zstack.header.host.HostVO.class)
    private java.util.List<String> hostUuids;
    public java.util.List<String> getHostUuids() { return hostUuids; }
    public void setHostUuids(java.util.List<String> value) { hostUuids = value; }

    @APIParam(required = false, resourceType = org.zstack.header.zone.ZoneVO.class)
    private String zoneUuid;
    public String getZoneUuid() { return zoneUuid; }
    public void setZoneUuid(String value) { zoneUuid = value; }

    public static APIGetMemorySummaryMsg __example__() {
        APIGetMemorySummaryMsg msg = new APIGetMemorySummaryMsg();
        msg.setHostUuids(java.util.Collections.singletonList("1234567890abcdef1234567890abcdef"));
        msg.setZoneUuid("1234567890abcdef1234567890abcdef");
        return msg;
    }
}
