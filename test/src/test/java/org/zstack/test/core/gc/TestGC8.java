package org.zstack.test.core.gc;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.gc.*;
import org.zstack.test.BeanConstructor;
import org.zstack.test.DBUtil;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 8/5/2015.
 */
public class TestGC8 {
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
        CLogger logger = Utils.getLogger(TestGC8.class);

        @Override
        public void run(GCContext context, GCCompletion completion) {
            logger.debug((String)context.getContext());
            completion.success();
        }
    }

    @Test
    public void test() {
        for (int i=0; i<5; i++) {
            TimeBasedGCPersistentContext<String> context = new TimeBasedGCPersistentContext<String>();
            context.setRunnerClass(TRunner.class);
            context.setInterval(1000000);
            context.setTimeUnit(TimeUnit.SECONDS);
            context.setContext("this is a string");
            context.setContextClass(String.class);
            context.setName("test");
            gcf.scheduleImmediately(context);
        }

        SimpleQuery q = dbf.createQuery(GarbageCollectorVO.class);
        q.add(GarbageCollectorVO_.id, Op.IN, list(Long.valueOf(1), Long.valueOf(2)));
        List<GarbageCollectorVO> vos = q.list();
    }
}
