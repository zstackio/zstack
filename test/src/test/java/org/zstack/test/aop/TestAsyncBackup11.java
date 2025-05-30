package org.zstack.test.aop;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
/**
 */
public class TestAsyncBackup11 {
    CLogger logger = Utils.getLogger(TestAsyncBackup11.class);
    boolean success;
    ComponentLoader loader;
    ThreadFacade thdf;
    CloudBus bus;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        thdf = loader.getComponent(ThreadFacade.class);
        bus = loader.getComponent(CloudBus.class);
    }

    @Test
    public void test() throws InterruptedException {
        Completion comp = new Completion(null) {
            @Override
            public void success() {
            }

            @Override
            public void fail(ErrorCode errorCode) {
                success = true;
            }
        };
        Future<Void> task = thdf.submitCancelablePeriodicTask(new CancelablePeriodicTask(comp) {
            @Override
            public boolean run() {
                throw new RuntimeException("on purpose");
            }

            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return 3;
            }

            @Override
            public String getName() {
                return "test";
            }
        });

        TimeUnit.SECONDS.sleep(1);
        Assert.assertTrue(success);
        Assert.assertTrue(task.isCancelled());
    }
}
