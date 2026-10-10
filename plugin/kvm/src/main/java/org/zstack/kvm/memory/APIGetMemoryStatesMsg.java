package org.zstack.kvm.memory;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.host.HostConstant;
import org.zstack.header.rest.RestRequest;

/** Clear-verb alias for the legacy APIQueryMemoryStateMsg route. */
@Action(category = HostConstant.ACTION_CATEGORY, adminOnly = true, names = {"read"})
@RestRequest(path = "/memory-optimization/states", method = HttpMethod.GET,
        responseClass = APIQueryMemoryStateReply.class, strictQueryParameters = true)
public class APIGetMemoryStatesMsg extends APIQueryMemoryStateMsg {
    public static APIGetMemoryStatesMsg __example__() {
        APIGetMemoryStatesMsg msg = new APIGetMemoryStatesMsg();
        msg.setHostUuids(java.util.Collections.singletonList("1234567890abcdef1234567890abcdef"));
        msg.setStart(0);
        msg.setLimit(100);
        return msg;
    }
}
