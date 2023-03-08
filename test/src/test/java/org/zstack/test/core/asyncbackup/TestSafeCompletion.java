package org.zstack.test.core.asyncbackup;

import org.junit.Test;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.header.core.HaCheckerCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

/**
 * Created by MaJin on 2021/11/1.
 */
public class TestSafeCompletion {
    CLogger logger = Utils.getLogger(TestSafeCompletion.class);
    int succCount, failCount, noWayCount, notStableCount;


    HaCheckerCompletion haCompletion = new HaCheckerCompletion(null) {
        @Override
        public void success(Object returnValue) {
            logger.debug("success");
            succCount++;
        }

        @Override
        public void fail(ErrorCode errorCode) {
            logger.debug("fail");
            failCount++;
        }

        @Override
        public void noWay() {
            logger.debug("noWay");
            noWayCount++;
        }

        @Override
        public void notStable() {
            logger.debug("notStable");
            notStableCount++;
        }
    };

    @Test
    public void testSafeCompletion() {
        // *(.., HaCheckerCompletion, ..) will catch the exception, and call noWay.
        throwException(haCompletion);
        assert noWayCount == 1;
        assert succCount == 0 && failCount == 0 && notStableCount == 0;
        reset();

        // *(.., HaCheckerCompletion, ..) will catch the exception, but function only called once.
        throwExceptionAndCallSucc(haCompletion);
        assert succCount == 1;
        assert noWayCount == 0 && failCount == 0 && notStableCount == 0;
        reset();

        // other async backup exception will call noWay.
        new CloudBusCallBack(haCompletion) {
            @Override
            public void run(MessageReply reply) {
                haCompletion.success(null);
                throw new OperationFailureException(operr("on purpose 3"));
            }
        }.run(null);
        assert succCount == 1;
        assert noWayCount == 0 && failCount == 0 && notStableCount == 0;
    }

    private void throwException(HaCheckerCompletion completion) {
        throw new OperationFailureException(operr("on purpose 1"));
    }

    private void throwExceptionAndCallSucc(HaCheckerCompletion completion) {
        completion.success(null);
        completion.noWay();
        completion.fail(null);
        completion.notStable();
        throw new OperationFailureException(operr("on purpose 2"));
    }

    private void reset() {
        succCount = 0;
        failCount = 0;
        noWayCount = 0;
        notStableCount = 0;
        haCompletion.getFailCalled().set(false);
    }
}
