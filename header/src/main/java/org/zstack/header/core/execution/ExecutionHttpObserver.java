package org.zstack.header.core.execution;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

/** Observes outbound HTTP child stages under the current execution context. */
public interface ExecutionHttpObserver {
    /**
     * Record an outbound HTTP request under the current execution context.
     * Implementations must extract what they need synchronously and must not
     * retain the request entity.
     */
    String recordHttpRequestStarted(HttpMethod method, String url, HttpEntity<?> request);

    /** Record the terminal result of an outbound HTTP request. */
    void recordHttpRequestCompleted(String requestUuid, String state, Integer statusCode, String error);
}
