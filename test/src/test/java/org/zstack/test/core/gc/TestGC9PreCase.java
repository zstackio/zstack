package org.zstack.test.core.gc;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBusImpl2;
import org.zstack.core.cloudbus.EventFacadeImpl;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.gc.*;
import org.zstack.test.BeanConstructor;
import org.zstack.test.DBUtil;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

/**
 * test event based GC
 */
public class TestGC9PreCase {
    ComponentLoader loader;
    GCFacade gcf;
    DatabaseFacade dbf;
    EventFacadeImpl evtf;
    CloudBusImpl2 bus;
    static boolean success;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        gcf = loader.getComponent(GCFacade.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        evtf = loader.getComponent(EventFacadeImpl.class);
        bus = loader.getComponent(CloudBusImpl2.class);
        evtf.start();
    }

    public static class TRunner implements GCRunner {
        CLogger logger = Utils.getLogger(TestGC9PreCase.class);

        @Override
        public void run(GCContext context, GCCompletion completion) {
            logger.debug((String)context.getContext());
            success = true;
            completion.success();
        }
    }

    private void scheduleJobs(String name) {
        String eventPath = "/test/gc";
        EventBasedGCPersistentContext<String> ctx = new EventBasedGCPersistentContext<String>();
        ctx.setContextClass(String.class);
        ctx.setRunnerClass(TRunner.class);
        ctx.setName(name);
        ctx.setContext("I am running");

        GCEventTrigger trigger = new GCEventTrigger();
        trigger.setEventPath(eventPath);
        trigger.setCodeName("test-gc-code");

        String code = "if (data == \"hello\") {" +
                "   return true;" +
                "} else {" +
                "   return false;" +
                "}";

        trigger.setCode(code);
        ctx.addTrigger(trigger);

        gcf.schedule(ctx);
    }

    @Test
    public void test() throws InterruptedException {
        for (int i=0; i<50; i++) {
            scheduleJobs(String.format("test-gc-%s", i));
        }

        List<GarbageCollectorVO> vos = dbf.listAll(GarbageCollectorVO.class);
        for (GarbageCollectorVO vo : vos) {
            vo.setManagementNodeUuid(null);
        }
        dbf.updateCollection(vos);
    }
}
