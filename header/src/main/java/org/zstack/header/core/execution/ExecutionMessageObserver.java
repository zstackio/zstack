package org.zstack.header.core.execution;

import org.zstack.header.message.Message;

/**
 * Observes CloudBus message lifecycle facts.
 *
 * <p>The observer receives the original message so it can evolve the fields it
 * extracts without changing CloudBus call sites. Implementations must not retain
 * the mutable message instance.</p>
 */
public interface ExecutionMessageObserver {
    /** Record delivery of a request message. */
    void recordMessageStarted(Message message);

    /** Record a reply or another terminal outcome of the original request. */
    void recordMessageCompleted(Message message, String state, String reason);
}
