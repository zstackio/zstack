package org.zstack.longjob;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.db.Q;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Constants;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.longjob.LongJobState;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.longjob.LongJobVO_;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class LongJobTrackerImpl implements LongJobTracker, ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(LongJobTrackerImpl.class);
    private static final List<LongJobState> stableStates = Arrays.asList(
            LongJobState.Canceled,
            LongJobState.Failed,
            LongJobState.Succeeded
    );

    private final ConcurrentHashMap<String, Consumer<LongJobVO>> listeners = new ConcurrentHashMap<>();

    @Autowired
    private ThreadFacade thdf;

    @Override
    public void registerLongJobListener(String jobUuid, Consumer<LongJobVO> listener, String resourceType) {
        if (listeners.putIfAbsent(jobUuid, listener) == null) {
            String apiId = ThreadContext.get(Constants.THREAD_CONTEXT_API);
            logger.info(String.format("tracking %s job: %s, api=%s", resourceType, jobUuid, apiId));
        }
    }

    @Override
    public void managementNodeReady() {
        thdf.submitPeriodicTask(new PeriodicTask() {
            private boolean running = false;

            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return CoreGlobalProperty.UNIT_TEST_ON ? 1 : 7;
            }

            @Override
            public String getName() {
                return "longjob-tracker";
            }

            @Override
            @ExceptionSafe
            public void run() {
                final Set<String> jobUuids = listeners.keySet();
                if (jobUuids.isEmpty() || running) {
                    return;
                }

                try {
                    running = true;
                    List<LongJobVO> jobs = Q.New(LongJobVO.class)
                            .in(LongJobVO_.uuid, jobUuids)
                            .in(LongJobVO_.state, stableStates)
                            .list();
                    for (LongJobVO jobVO : jobs) {
                        Consumer<LongJobVO> c = listeners.remove(jobVO.getUuid());
                        logger.info(String.format("job: name=%s, uuid=%s, state=%s", jobVO.getJobName(), jobVO.getUuid(), jobVO.getState()));
                        c.accept(jobVO);
                    }
                } finally {
                    running = false;
                }
            }
        });
    }
}
