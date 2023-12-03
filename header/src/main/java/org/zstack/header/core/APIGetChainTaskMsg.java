package org.zstack.header.core;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

import java.util.List;
import java.util.function.Function;

@Action(category = CoreConstant.ACTION_CATEGORY, adminOnly = true)
@RestRequest(
        path = "/core/task-details",
        method = HttpMethod.GET,
        responseClass = APIGetChainTaskReply.class
)
public class APIGetChainTaskMsg extends APISyncCallMessage {
    @APIParam(nonempty = false, required = false)
    private List<String> syncSignatures;

    public void setSyncSignatures(List<String> syncSignatures) {
        this.syncSignatures = syncSignatures;
    }

    public List<String> getSyncSignatures() {
        return syncSignatures;
    }

    public Function<String, String> getResourceUuidMaker() {
        return null;
    }
}
