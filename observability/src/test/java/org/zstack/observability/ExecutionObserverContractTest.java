package org.zstack.observability;

import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.zstack.header.core.execution.ExecutionHttpObserver;
import org.zstack.header.core.execution.ExecutionMessageObserver;
import org.zstack.header.core.execution.ExecutionScheduledTaskObserver;
import org.zstack.header.message.Message;

/**
 * Locks the observer boundary to domain objects. Execution observability may
 * extract more fields later without changing every producer again.
 */
public class ExecutionObserverContractTest {
    @Test
    public void observersAcceptOriginalSourcesAtLifecycleBoundaries() throws NoSuchMethodException {
        ExecutionMessageObserver.class.getMethod("recordMessageStarted", Message.class);
        ExecutionMessageObserver.class.getMethod(
                "recordMessageCompleted", Message.class, String.class, String.class);

        ExecutionHttpObserver.class.getMethod(
                "recordHttpRequestStarted", HttpMethod.class, String.class, HttpEntity.class);
        ExecutionHttpObserver.class.getMethod(
                "recordHttpRequestCompleted", String.class, String.class, Integer.class, String.class);

        ExecutionScheduledTaskObserver.class.getMethod("recordScheduledTaskStarted", Object.class);
        ExecutionScheduledTaskObserver.class.getMethod(
                "recordScheduledTaskCompleted", String.class, Object.class, Throwable.class);
    }
}
