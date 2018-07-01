package org.zstack.query;

import org.zstack.header.message.APIReply;
import org.zstack.header.message.NoJsonSchema;
import org.zstack.header.rest.RestResponse;
import org.zstack.zql.ZQLQueryReturn;

@RestResponse(allTo = "result")
public class APIZQLQueryReply extends APIReply {
    @NoJsonSchema
    private ZQLQueryReturn result;

    public static APIZQLQueryReply __example__() {
        APIZQLQueryReply ret = new APIZQLQueryReply();
        return ret;
    }

    public ZQLQueryReturn getResult() {
        return result;
    }

    public void setResult(ZQLQueryReturn result) {
        this.result = result;
    }
}
