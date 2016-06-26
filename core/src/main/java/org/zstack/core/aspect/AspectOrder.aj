package org.zstack.core.aspect;

/**
 */
public aspect AspectOrder {
    declare precedence : ExceptionSafeAspect, ThreadAspect, MessageSafeAspect, AsyncSafeAspect, AsyncBackupAspect,
            CompletionSingleCallAspect, DbDeadlockAspect, DeferAspect, AnnotationTransactionAspect;
}
