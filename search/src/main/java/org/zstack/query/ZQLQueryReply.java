package org.zstack.query;

import org.zstack.header.message.APIReply;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NoJsonSchema;
import org.zstack.header.rest.RestResponse;
import org.zstack.zql.ZQLQueryReturn;

import java.util.List;

public class ZQLQueryReply extends MessageReply {
    private List<ZQLQueryReturn> results;

    public List<ZQLQueryReturn> getResults() {
        return results;
    }

    public void setResults(List<ZQLQueryReturn> results) {
        this.results = results;
    }
}
