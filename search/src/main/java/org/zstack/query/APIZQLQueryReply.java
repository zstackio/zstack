package org.zstack.query;

import org.zstack.header.message.APIReply;
import org.zstack.header.message.NoJsonSchema;
import org.zstack.header.rest.RestResponse;
import org.zstack.zql.ZQLQueryResult;

@RestResponse(allTo = "result")
public class APIZQLQueryReply extends APIReply {
    @NoJsonSchema
    private ZQLQueryResult result;

    public static APIZQLQueryReply __example__() {
        APIZQLQueryReply ret = new APIZQLQueryReply();
        return ret;
    }

    public ZQLQueryResult getResult() {
        return result;
    }

    public void setResult(ZQLQueryResult result) {
        this.result = result;
    }
}
