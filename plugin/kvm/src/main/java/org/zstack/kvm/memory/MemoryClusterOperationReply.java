package org.zstack.kvm.memory;

import org.zstack.header.message.MessageReply;
import java.util.Map;

/** hostUuid to "success", or the Host's final status and reason. */
public class MemoryClusterOperationReply extends MessageReply {
    private Map<String, String> results;
    public Map<String, String> getResults() { return results; }
    public void setResults(Map<String, String> value) { results = value; }
}
