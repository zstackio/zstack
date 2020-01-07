package org.zstack.core.thread;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.Constants;
import org.zstack.header.core.AsyncBackup;
import org.zstack.header.core.progress.PendingTaskInfo;
import org.zstack.header.core.progress.RunningTaskInfo;
import org.zstack.header.core.progress.TaskInfo;
import org.zstack.header.message.Message;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by MaJin on 2019/7/4.
 */
public class TaskInfoBuilder {
    static RunningTaskInfo buildRunningTaskInfo(DispatchQueueImpl.ChainFuture cf, long now, int index) {
        RunningTaskInfo info = new RunningTaskInfo();
        loadTaskInfo(info, cf, index);
        info.setExecutionTime(TimeUnit.MILLISECONDS.toSeconds(now - cf.getStartExecutionTimeInMills()));
        info.setPendingTime(TimeUnit.MILLISECONDS.toSeconds(now - cf.getStartPendingTimeInMills()) - info.getExecutionTime());
        return info;

    }

    static PendingTaskInfo buildPendingTaskInfo(DispatchQueueImpl.ChainFuture cf, long now, int index) {
        PendingTaskInfo info = new PendingTaskInfo();
        loadTaskInfo(info, cf, index);
        info.setPendingTime(TimeUnit.MILLISECONDS.toSeconds(now - cf.getStartPendingTimeInMills()));
        return info;
    }

    static private void loadTaskInfo(TaskInfo info, DispatchQueueImpl.ChainFuture cf, int index) {
        info.setName(cf.getTask().getName());
        info.setClassName(cf.getTask().getClass().getSimpleName());
        info.setIndex(index);
        Map<String, String> tc = cf.getTask().getThreadContext();
        if (tc != null) {
            info.setApiName(tc.get(Constants.THREAD_CONTEXT_TASK_NAME));
            info.setApiId(tc.get(Constants.THREAD_CONTEXT_API));
        }

        info.setContextList(new ArrayList<>());
        for (AsyncBackup backup : cf.getTask().getBackups()) {
            if (backup instanceof Message) {
                info.getContextList().add(JSONObjectUtil.toJsonString(backup));
            }
        }

        info.setContext(info.getContextList().isEmpty() ? "\n" : String.format("\nCONTEXT: %s\n", StringUtils.join(info.getContextList(), "\n")));
    }
}
