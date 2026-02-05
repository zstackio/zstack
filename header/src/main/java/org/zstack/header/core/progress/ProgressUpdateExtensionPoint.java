package org.zstack.header.core.progress;

/**
 * Extension point invoked after a task progress record is persisted.
 * Used to trigger downstream notifications (e.g. Long Job progress to SNS) without coupling to progress storage.
 */
public interface ProgressUpdateExtensionPoint {
    void afterProgressPersisted(TaskProgressVO vo);
}
