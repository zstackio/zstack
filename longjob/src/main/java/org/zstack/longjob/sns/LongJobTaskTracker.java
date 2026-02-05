package org.zstack.longjob.sns;

import org.zstack.core.progress.TaskTracker;
import org.zstack.header.longjob.LongJobVO;

/**
 * Task tracker for Long Job state change and progress update notifications.
 * Used by LongJobProgressNotification to receive tasks and publish to SNS.
 */
public class LongJobTaskTracker extends TaskTracker {
    public static final String TASK_NAME = "longjob-progress";

    public static final String PARAM_STATE = "state";
    public static final String PARAM_PROGRESS = "progress";
    public static final String PARAM_LONG_JOB_UUID = "longJobUuid";
    public static final String PARAM_JOB_NAME = "jobName";

    public enum EventType {
        STATE_CHANGED,
        PROGRESS_UPDATED
    }

    public LongJobTaskTracker(String resourceUuid) {
        super(resourceUuid);
    }

    @Override
    protected String getResourceType() {
        return LongJobVO.class.getSimpleName();
    }

    @Override
    protected String getTaskName() {
        return TASK_NAME;
    }
}
