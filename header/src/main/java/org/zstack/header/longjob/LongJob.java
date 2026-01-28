package org.zstack.header.longjob;

import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.message.APIEvent;

/**
 * Created by GuoYi on 11/24/17.
 */
public interface LongJob {
    void start(LongJobVO job, ReturnValueCompletion<APIEvent> completion);
    default void cancel(LongJobVO job, ReturnValueCompletion<Boolean> completion) {}

    /**
     * Suspend a running long job.
     * <p>
     * This method is called by the long job framework when a suspend request is received.
     * The default implementation is a no-op (does nothing), which means jobs that don't
     * override this method will not support suspend operation.
     * <p>
     * Implementations should:
     * <ul>
     *   <li>Perform the actual suspend logic (e.g., pause ongoing operations, save state)</li>
     *   <li>Call {@code completion.success(Boolean.TRUE)} if suspend succeeds</li>
     *   <li>Call {@code completion.fail(ErrorCode)} if suspend fails</li>
     * </ul>
     * <p>
     * Note: The framework will check if the job type supports suspend via
     * {@link LongJobFactory#supportSuspend(String)} before calling this method.
     *
     * @param job the long job to suspend
     * @param completion the completion callback. Must call {@code success(Boolean.TRUE)}
     *                   on successful suspend, or {@code fail(ErrorCode)} on failure.
     *                   The Boolean value indicates whether the job was successfully suspended.
     */
    default void suspend(LongJobVO job, ReturnValueCompletion<Boolean> completion) {}

    default void resume(LongJobVO job, ReturnValueCompletion<APIEvent> completion) {}
    default void clean(LongJobVO job, NoErrorCompletion completion) {}
    default Class getAuditType() {
        return null;
    }
    default String getAuditResourceUuid() {
        return null;
    }
}
