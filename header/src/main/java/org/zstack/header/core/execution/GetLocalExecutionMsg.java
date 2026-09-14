package org.zstack.header.core.execution;

import org.zstack.header.message.NeedReplyMessage;

/** Internal CloudBus request used to collect execution records from one management node. */
public class GetLocalExecutionMsg extends NeedReplyMessage {
    private APIQueryExecutionMsg query;

    public APIQueryExecutionMsg getQuery() {
        return query;
    }

    public void setQuery(APIQueryExecutionMsg query) {
        this.query = query;
    }
}
