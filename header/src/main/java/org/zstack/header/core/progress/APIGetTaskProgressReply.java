package org.zstack.header.core.progress;

import org.zstack.header.message.APIReply;

import java.util.ArrayList;
import java.util.List;
/**
 * Created by mingjian.deng on 16/12/8.
 */
public class APIGetTaskProgressReply extends APIReply {
    private List<TaskProgress> taskProgress;

    public List<TaskProgress> getTaskProgress() {
        if (taskProgress == null) {
            taskProgress = new ArrayList<>();
        }
        return taskProgress;
    }

    public void setTaskProgress(List<TaskProgress> taskProgress) {
        this.taskProgress = taskProgress;
    }
}
