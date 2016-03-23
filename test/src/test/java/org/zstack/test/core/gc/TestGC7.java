package org.zstack.test.core.gc;

import junit.framework.Assert;
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

import java.util.concurrent.TimeUnit;

import static org.zstack.utils.StringDSL.ln;

/**
 * test event based GC
 */
public class TestGC7 {
    ComponentLoader loader;
    GCFacade gcf;
    DatabaseFacade dbf;
    EventFacadeImpl evtf;
    CloudBusImpl2 bus;
    static int success;

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
        CLogger logger = Utils.getLogger(TestGC7.class);

        @Override
        public void run(GCContext context, GCCompletion completion) {
            logger.debug((String)context.getContext());
            success ++;
            completion.success();
        }
    }

    @Test
    public void test() throws InterruptedException {
        String eventPath = "/test/gc";
        EventBasedGCEphemeralContext<String> ctx = new EventBasedGCEphemeralContext<String>();
        ctx.setName("test-gc");
        ctx.setContext("I am running");
        ctx.setRunner(new TRunner());

        GCEventTrigger trigger = new GCEventTrigger();
        trigger.setEventPath(eventPath);
        trigger.setCodeName("test-gc-code");

        String code = ln(
                "println tokens.toString()",
                "println context.toString()",
                "println data.toString()",
                "return data == \"hello\" || data == \"world\""
        ).toString();

        trigger.setCode(code);
        ctx.addTrigger(trigger);
        gcf.schedule(ctx);

        evtf.fire(eventPath, "not run");
        TimeUnit.SECONDS.sleep(1);
        Assert.assertEquals(0, success);

        evtf.fire(eventPath, "hello");
        TimeUnit.SECONDS.sleep(1);
        Assert.assertEquals(1, success);

        evtf.fire(eventPath, "hello");
        evtf.fire(eventPath, "hello");
        evtf.fire(eventPath, "world");
        TimeUnit.SECONDS.sleep(1);
        Assert.assertEquals(1, success);
    }
}
