package org.zstack.observability;

import org.zstack.header.core.execution.APIQueryExecutionMsg;
import org.zstack.header.message.APIGetMessage;
import org.zstack.header.message.APIListMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.search.APISearchMessage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Decides which public API requests should create a root execution record.
 * Read-only APIs are intentionally excluded from the execution stream; their
 * internal Message stages remain observable when they are children of a
 * mutating API execution.
 */
public class ExecutionObservationPolicy {
    static final List<String> IGNORED_API_CLASS_NAMES = Collections.unmodifiableList(Arrays.asList(
            "org.zstack.query.APIZQLQueryMsg"
    ));

    public boolean shouldObserveApi(APIMessage message) {
        if (message == null) {
            return false;
        }

        if (IGNORED_API_CLASS_NAMES.contains(message.getClass().getName())
                || message instanceof APIQueryExecutionMsg
                || message instanceof APIQueryMessage
                || message instanceof APISearchMessage
                || message instanceof APIListMessage
                || message instanceof APIGetMessage
                || message instanceof org.zstack.header.search.APIGetMessage) {
            return false;
        }

        // A number of legacy APIGet/APIQuery messages predate the common base
        // classes and extend APISyncCallMessage directly. Keep those APIs out
        // of the root execution stream without requiring a module-wide list.
        String simpleName = message.getClass().getSimpleName();
        return !simpleName.startsWith("APIGet")
                && !simpleName.startsWith("APIQuery")
                && !simpleName.startsWith("APISearch");
    }
}
