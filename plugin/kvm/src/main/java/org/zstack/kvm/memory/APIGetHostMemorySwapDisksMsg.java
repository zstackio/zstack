package org.zstack.kvm.memory;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostConstant;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

@Action(category = HostConstant.ACTION_CATEGORY, adminOnly = true, names = {"read"})
@RestRequest(path = "/hosts/{hostUuid}/memory-swap-disks", method = HttpMethod.GET,
        responseClass = APIGetHostMemorySwapDisksReply.class)
public class APIGetHostMemorySwapDisksMsg extends APISyncCallMessage {
    @APIParam(resourceType = HostVO.class)
    private String hostUuid;

    public String getHostUuid() { return hostUuid; }
    public void setHostUuid(String hostUuid) { this.hostUuid = hostUuid; }

    public static APIGetHostMemorySwapDisksMsg __example__() {
        APIGetHostMemorySwapDisksMsg msg = new APIGetHostMemorySwapDisksMsg();
        msg.setHostUuid("1234567890abcdef1234567890abcdef");
        return msg;
    }
}
