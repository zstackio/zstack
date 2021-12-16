package org.zstack.core.singleflight;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.Constants;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.progress.RunningTaskInfo;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.utils.TimeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by Wenhao.Zhang on 21/08/10
 */
@Deprecated
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class TaskSingleFlight<K, V> extends CompletionSingleFlight<K, V> {
    private static final CLogger logger = Utils.getLogger(TaskSingleFlight.class);

    @Override
    protected SingleFlightContext buildContext(K key) {
        return new TaskSingleFlightContext();
    }

    @SuppressWarnings("unchecked")
    public RunningTaskInfo buildRunningTaskInfo(K key) {
        SingleFlightContext c = getContext(key);
        if (c == null) {
            return null;
        }
        TaskSingleFlightContext context = (TaskSingleFlightContext) c;
        final Instant now = Instant.now();

        final RunningTaskInfo info = new RunningTaskInfo();
        info.setExecutionTime(Duration.between(context.getStartTime(), now).getSeconds());
        info.setPendingTime(0);
        info.setName(String.valueOf(key));
        info.setClassName(TaskSingleFlightContext.class.getSimpleName());
        info.setIndex(0);

        ReturnValueCompletion<V> first = context.getFirst();
        if (first != null) {
            Map<String, String> threadContext = first.getThreadContext();
            if (threadContext != null) {
                info.setApiName(threadContext.get(Constants.THREAD_CONTEXT_TASK_NAME));
                info.setApiId(threadContext.get(Constants.THREAD_CONTEXT_API));
            }
        }
        info.setContextList(Collections.emptyList());
        info.setContext(map(e("apiName", info.getApiName()), e("apiId", info.getApiId())).toString());
        return info;
    }

    @Override
    protected void onFirstStart(K key, SingleFlightContext context) {
        logger.debug(String.format("running task: %s", buildRunningTaskInfo(key)));
    }

    @Override
    protected void onNextPending(K key, SingleFlightContext context) {
        TaskSingleFlightContext taskContext = (TaskSingleFlightContext) context;
        logger.debug(String.format("pending task %s: waiting for %s (start at %s), pending tasks count: %d", key,
                buildRunningTaskInfo(key), TimeUtils.instantToString(taskContext.getStartTime()), context.pendingCount()));
    }

    @Override
    protected void onSuccess(K key, SingleFlightContext context, V v) {
        logger.debug(String.format("task %s accomplished successfully: %s, pending tasks count: %d", key,
                buildRunningTaskInfo(key), context.pendingCount()));
    }

    @Override
    protected void onFail(K key, SingleFlightContext context, ErrorCode errorCode) {
        logger.warn(String.format("task %s fail because %s: %s, pending tasks count %d", key, errorCode.getDetails(),
                buildRunningTaskInfo(key), context.pendingCount()));
    }

    class TaskSingleFlightContext extends SingleFlightContext {
        private Instant startTime = Instant.now();

        public Instant getStartTime() {
            return startTime;
        }

        public boolean isPending() {
            return this.startTime == null;
        }
    }
}
