package org.zstack.header.host;

import org.zstack.header.core.progress.ChainInfo;
import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by MaJin on 2019/7/3.
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetHostTaskReply extends APIReply {
    private Map<String, ChainInfo> results = new HashMap<>();

    public Map<String, ChainInfo> getResults() {
        return results;
    }

    public void setResults(Map<String, ChainInfo> results) {
        this.results = results;
    }

    public void putResults(String hostUuid, ChainInfo info) {
        results.put(hostUuid, info);
    }
}
