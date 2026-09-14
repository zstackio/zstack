package org.zstack.header.core.execution;

/** Observes individual invocations of scheduled and timer tasks. */
public interface ExecutionScheduledTaskObserver {
    /**
     * Record one invocation of the original scheduled task object. The core
     * scheduler has several unrelated task interfaces, so Object is the only
     * common boundary type. Implementations must not retain it.
     */
    String recordScheduledTaskStarted(Object task);

    /**
     * Complete a previously recorded scheduled-task invocation and restore its
     * task-thread context.
     *
     * @param executionUuid execution UUID returned by {@link #recordScheduledTaskStarted(Object)}
     * @param task original scheduled task object
     * @param error failure thrown by the task, or {@code null} on success
     */
    void recordScheduledTaskCompleted(String executionUuid, Object task, Throwable error);
}
