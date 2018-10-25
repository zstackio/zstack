package org.zstack.longjob;

import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.longjob.LongJobErrors;
import org.zstack.header.longjob.LongJobState;
import org.zstack.header.longjob.LongJobVO;
import org.zstack.header.longjob.LongJobVO_;

import java.util.Arrays;
import java.util.List;

public class LongJobUtils {
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
}
