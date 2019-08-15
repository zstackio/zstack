package org.zstack.header.host;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

import java.util.List;

/**
 * Created by MaJin on 2019/7/3.
 */

@Action(category = HostConstant.ACTION_CATEGORY, adminOnly = true)
@RestRequest(
        path = "/hosts/task-details",
        method = HttpMethod.GET,
        responseClass = APIGetHostTaskReply.class
)
public class APIGetHostTaskMsg extends APISyncCallMessage {
    @APIParam(nonempty = true, resourceType = HostVO.class)
    private List<String> hostUuids;

    public List<String> getHostUuids() {
        return hostUuids;
    }

    public void setHostUuids(List<String> hostUuids) {
        this.hostUuids = hostUuids;
    }
}
