package org.zstack.test.core.gc;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.gc.*;
import org.zstack.test.BeanConstructor;
import org.zstack.test.DBUtil;
import org.zstack.utils.TimeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * only succeed after executing 3 times
 */
public class TestGC5 {
    ComponentLoader loader;
    GCFacade gcf;
    DatabaseFacade dbf;
    boolean success;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        gcf = loader.getComponent(GCFacade.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    public static class TRunner implements GCRunner {
        CLogger logger = Utils.getLogger(TestGC5.class);

        @Override
        public void run(GCContext context, GCCompletion completion) {
            logger.debug((String) context.getContext());
            if (context.getExecutedTimes() > 3) {
                completion.success();
            } else {
                completion.fail(null);
            }
        }
    }

    @Test
    public void test() {
        TimeBasedGCPersistentContext<String> context = new TimeBasedGCPersistentContext<String>();
        context.setRunnerClass(TRunner.class);
        context.setInterval(1);
        context.setTimeUnit(TimeUnit.SECONDS);
        context.setContext("this is a string");
        context.setContextClass(String.class);
        context.setName("test");

        gcf.scheduleImmediately(context);

        TimeUtils.loopExecuteUntilTimeoutIgnoreException(5, 1, TimeUnit.SECONDS, new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                GarbageCollectorVO vo = dbf.listAll(GarbageCollectorVO.class).get(0);
                if (vo.getStatus() == GCStatus.Done) {
                    success = true;
                    return true;
                }
                return false;
            }
        });

        Assert.assertTrue(success);
    }
}
