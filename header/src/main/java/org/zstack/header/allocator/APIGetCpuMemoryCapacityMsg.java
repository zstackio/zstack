package org.zstack.header.allocator;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.zone.ZoneVO;

import java.util.List;

@RestRequest(
        path = "/hosts/capacities/cpu-memory",
        method = HttpMethod.GET,
        responseClass = APIGetCpuMemoryCapacityReply.class,
        parameterName = "params"
)
public class APIGetCpuMemoryCapacityMsg extends APISyncCallMessage {
    @APIParam(required = false, resourceType = ZoneVO.class)
    private List<String> zoneUuids;
    @APIParam(required = false, resourceType = ClusterVO.class)
    private List<String> clusterUuids;
    @APIParam(required = false, resourceType = HostVO.class)
    private List<String> hostUuids;
    private boolean all;

    public boolean isAll() {
        return all;
    }

    public void setAll(boolean all) {
        this.all = all;
    }

    public List<String> getZoneUuids() {
        return zoneUuids;
    }

    public void setZoneUuids(List<String> zoneUuids) {
        this.zoneUuids = zoneUuids;
    }

    public List<String> getClusterUuids() {
        return clusterUuids;
    }

    public void setClusterUuids(List<String> clusterUuids) {
        this.clusterUuids = clusterUuids;
    }

    public List<String> getHostUuids() {
        return hostUuids;
    }

    public void setHostUuids(List<String> hostUuids) {
        this.hostUuids = hostUuids;
    }
}
