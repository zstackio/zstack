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
    static RunningTaskInfo buildRunningTaskInfo(AbstractTimeStatisticFuture abstractTimeStatisticFuture, long now, int index) {
        RunningTaskInfo info = new RunningTaskInfo();
        loadTaskInfo(info, abstractTimeStatisticFuture, index);
        info.setExecutionTime(TimeUnit.MILLISECONDS.toSeconds(now - abstractTimeStatisticFuture.getStartExecutionTimeInMills()));
        info.setPendingTime(TimeUnit.MILLISECONDS.toSeconds(now - abstractTimeStatisticFuture.getStartPendingTimeInMills()) - info.getExecutionTime());
        return info;
    }

    static PendingTaskInfo buildPendingTaskInfo(AbstractTimeStatisticFuture abstractTimeStatisticFuture, long now, int index) {
        PendingTaskInfo info = new PendingTaskInfo();
        loadTaskInfo(info, abstractTimeStatisticFuture, index);
        info.setPendingTime(TimeUnit.MILLISECONDS.toSeconds(now - abstractTimeStatisticFuture.getStartPendingTimeInMills()));
        return info;
    }

    static private void loadTaskInfo(TaskInfo info, AbstractTimeStatisticFuture cf, int index) {
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
