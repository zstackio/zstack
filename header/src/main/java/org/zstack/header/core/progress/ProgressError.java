package org.zstack.header.core.progress;

/**
 * Created by mingjian.deng on 16/12/13.
 */
public enum  ProgressError {
    NO_SUCH_TASK_RUNNING(1000);

    private String code;

    ProgressError(int id) {
        code = String.format("PROGRESS.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}