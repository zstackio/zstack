package org.zstack.core.aspect;

/**
 */
public aspect AspectOrder {
    declare precedence : ThreadAspect, MessageSafeAspect, AsyncSafeAspect, AsyncBackupAspect,
            CompletionSingleCallAspect, DbDeadlockAspect, DeferAspect, AnnotationTransactionAspect;
}
