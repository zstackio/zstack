package org.zstack.longjob;

import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.longjob.*;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.List;

public class LongJobUtils {
    private static final CLogger logger = Utils.getLogger(LongJobUtils.class);

    private static List<LongJobState> completedStates = Arrays.asList(LongJobState.Failed, LongJobState.Succeeded, LongJobState.Canceled);
    private static List<LongJobState> canceledStates = Arrays.asList(LongJobState.Canceled, LongJobState.Canceling);

    public static ErrorCode cancelErr(String longJobUuid) {
        return Platform.err(LongJobErrors.CANCELED, "long job[uuid:%s] has been canceled", longJobUuid);
    }

    public static ErrorCode cancelErr(String longJobUuid, ErrorCode cause) {
        ErrorFacade errf = Platform.getComponentLoader().getComponent(ErrorFacade.class);
        return errf.instantiateErrorCode(LongJobErrors.CANCELED,
                Platform.i18n("long job[uuid:%s] has been canceled", longJobUuid), cause);
    }


    public static boolean jobCanceled(String longJobUuid) {
        LongJobState state = Q.New(LongJobVO.class).eq(LongJobVO_.uuid,longJobUuid).select(LongJobVO_.state).findValue();
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

                if (job.getExecuteTime() == null && jobCompleted(job)) {
                    long time = (System.currentTimeMillis() - job.getCreateDate().getTime()) / 1000;
                    job.setExecuteTime(time);
                    logger.info(String.format("longjob [uuid:%s] set execute time:%d.", job.getUuid(), time));
                }

                merge(job);
                return job;
            }
        }.execute();
    }
}
