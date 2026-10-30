package org.zstack.kvm.memory;

import org.zstack.header.message.APIBatchRequest;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** hostUuid to "success", or the Host's final status and reason. */
public class MemoryClusterOperationReply extends MessageReply {
    private Map<String, String> results;
    public Map<String, String> getResults() { return results; }
    public void setResults(Map<String, String> value) { results = value; }

    /** The LongJob result: each Host as the platform's batch operation result. */
    public static String jobResult(Map<String, String> outcomes) {
        List<APIBatchRequest.BatchOperationResult<Void>> results = new ArrayList<>();
        outcomes.forEach((host, outcome) -> {
            APIBatchRequest.BatchOperationResult<Void> result = new APIBatchRequest.BatchOperationResult<>();
            result.setUuid(host);
            result.setSuccess("success".equals(outcome));
            if (!result.isSuccess()) { result.setError(hostError(outcome).toErrorCode()); }
            results.add(result);
        });
        return JSONObjectUtil.toJsonString(Collections.singletonMap("results", results));
    }

    /** "CODE: detail" names a memory error; anything else is a failed Host operation. */
    static MemoryOperationException hostError(String outcome) {
        if ("Canceled".equals(outcome)) {
            return new MemoryOperationException("MEMORY_HOST_OPERATION_CANCELED", "the job was canceled before this Host");
        }
        int colon = outcome.indexOf(": ");
        String code = colon > 0 ? outcome.substring(0, colon) : outcome;
        String detail = colon > 0 ? outcome.substring(colon + 2) : outcome;
        if ("Skipped".equals(code)) { return new MemoryOperationException("MEMORY_HOST_OPERATION_SKIPPED", detail); }
        if (MemoryErrors.fromBusinessCode(code) != MemoryErrors.UNKNOWN) { return new MemoryOperationException(code, detail); }
        return new MemoryOperationException("MEMORY_HOST_OPERATION_FAILED", "Failed".equals(code) ? detail : outcome);
    }
}
