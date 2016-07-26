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
 * Created by frank on 8/5/2015.
 */
public class TestGC3 {
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

    public static class Data1 {
        long num;
    }

    public static class Data {
        String name;
        Data1 data;
    }

    public static class TRunner implements GCRunner {
        static int count = 0;
        CLogger logger = Utils.getLogger(TRunner.class);

        public void run(GCContext context, GCCompletion completion) {
            count ++;
            logger.debug(String.format("count: %s", count));
            if (count < 3) {
                completion.fail(null);
            } else {
                Data d = (Data) context.getContext();
                logger.debug(String.format("num: %s", d.data.num));
                completion.success();
            }
        }
    }

    @Test
    public void test() {
        TimeBasedGCPersistentContext<Data> context = new TimeBasedGCPersistentContext<Data>();
        context.setRunnerClass(TRunner.class);
        context.setInterval(1);
        context.setTimeUnit(TimeUnit.SECONDS);
        context.setName("test");
        Data d = new Data();
        d.name = "data";
        Data1 da = new Data1();
        da.num = 10;
        d.data = da;
        context.setContext(d);
        context.setContextClass(Data.class);

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
