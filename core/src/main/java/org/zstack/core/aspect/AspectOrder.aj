package org.zstack.core.aspect;

/**
 */
public aspect AspectOrder {
    declare precedence : ThreadAspect, MessageSafeAspect, AsyncBackupAspect, AsyncSafeAspect,
            CompletionSingleCallAspect, DbDeadlockAspect, AnnotationTransactionAspect;
}
