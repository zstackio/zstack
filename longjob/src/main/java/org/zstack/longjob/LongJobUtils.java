package org.zstack.longjob;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.progress.ProgressReportService;
import org.zstack.header.Constants;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.longjob.LongJobErrors;
import org.zstack.header.longjob.LongJobState;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.longjob.LongJobVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.List;

import static org.zstack.core.Platform.err;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LongJobUtils {
    private static final CLogger logger = Utils.getLogger(LongJobUtils.class);

    @Autowired
    private ProgressReportService progRpt;

    private static List<LongJobState> completedStates = Arrays.asList(LongJobState.Failed, LongJobState.Succeeded, LongJobState.Canceled);
    private static List<LongJobState> canceledStates = Arrays.asList(LongJobState.Canceled, LongJobState.Canceling);

    public static ErrorCode cancelErr(String longJobUuid) {
        return Platform.err(LongJobErrors.CANCELED, "long job[uuid:%s] has been canceled", longJobUuid);
    }

    public static ErrorCode cancelErr(String longJobUuid, ErrorCode cause) {
        return err(LongJobErrors.CANCELED, cause,
                "long job[uuid:%s] has been canceled", longJobUuid);
    }


    public static boolean jobCanceled(String longJobUuid) {
        LongJobState state = Q.New(LongJobVO.class).eq(LongJobVO_.uuid,longJobUuid).select(LongJobVO_.state).findValue();
        return canceledStates.contains(state);
    }

    public static boolean jobCanceled() {
        LongJobState state = Q.New(LongJobVO.class).select(LongJobVO_.state)
                .eq(LongJobVO_.apiId, ThreadContext.get(Constants.THREAD_CONTEXT_API))
                .findValue();
        return canceledStates.contains(state);
    }

    public static boolean jobCompleted(LongJobVO vo) {
        return completedStates.contains(vo.getState());
    }

    public static LongJobVO updateByUuid(String uuid, ForEachFunction<LongJobVO> consumer) {
        return new SQLBatchWithReturn<LongJobVO>(){

            @Override
            protected LongJobVO scripts() {
                LongJobVO job = findByUuid(uuid, LongJobVO.class);
                consumer.run(job);

                if (jobCompleted(job)) {
                    setExecuteTimeIfNeed(job);
                    cleanProgress(job);
                }


                merge(job);
                return job;
            }
        }.execute();
    }

    private static void setExecuteTimeIfNeed(LongJobVO job) {
        if (job.getExecuteTime() == null) {
            long time = (System.currentTimeMillis() - job.getCreateDate().getTime()) / 1000;
            job.setExecuteTime(time);
            logger.info(String.format("longjob [uuid:%s] set execute time:%d.", job.getUuid(), time));
        }
    }

    @ExceptionSafe
    private static void cleanProgress(LongJobVO job) {
        ProgressReportService progRpt = Platform.getComponentLoader().getComponent(ProgressReportService.class);
        progRpt.cleanTaskProgress(job.getApiId());
    }
}
