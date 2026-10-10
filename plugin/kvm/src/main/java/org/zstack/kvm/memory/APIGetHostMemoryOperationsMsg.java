package org.zstack.kvm.memory;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostConstant;
import org.zstack.header.identity.Action;
import org.zstack.header.rest.RestRequest;

/** Clear-verb alias for the legacy Host memory operations query route. */
@Action(category = HostConstant.ACTION_CATEGORY, adminOnly = true, names = {"read"})
@RestRequest(path = "/hosts/{hostUuid}/memory-optimization/operations", method = HttpMethod.GET,
        responseClass = APIQueryHostMemoryOperationsReply.class, strictQueryParameters = true)
public class APIGetHostMemoryOperationsMsg extends APIQueryHostMemoryOperationsMsg {
    public static APIGetHostMemoryOperationsMsg __example__() {
        APIGetHostMemoryOperationsMsg msg = new APIGetHostMemoryOperationsMsg();
        msg.setHostUuid("1234567890abcdef1234567890abcdef");
        msg.setStart(0);
        msg.setLimit(100);
        return msg;
    }
}
