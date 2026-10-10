package org.zstack.kvm.memory;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.host.HostConstant;
import org.zstack.header.rest.RestRequest;

/** Clear-verb alias for the legacy APIQueryMemoryTaskMsg route. */
@Action(category = HostConstant.ACTION_CATEGORY, adminOnly = true, names = {"read"})
@RestRequest(path = "/memory-optimization/tasks", method = HttpMethod.GET,
        responseClass = APIQueryMemoryTaskReply.class, strictQueryParameters = true)
public class APIGetMemoryTasksMsg extends APIQueryMemoryTaskMsg {
    public static APIGetMemoryTasksMsg __example__() {
        APIGetMemoryTasksMsg msg = new APIGetMemoryTasksMsg();
        msg.setStart(0);
        msg.setLimit(100);
        return msg;
    }
}
