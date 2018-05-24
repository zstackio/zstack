package org.zstack.query;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.zql.ZQLQueryResult;

@RestResponse(allTo = "result")
public class APIZQLQueryReply extends APIReply {
    private ZQLQueryResult result;

    public ZQLQueryResult getResult() {
        return result;
    }

    public void setResult(ZQLQueryResult result) {
        this.result = result;
    }
}
