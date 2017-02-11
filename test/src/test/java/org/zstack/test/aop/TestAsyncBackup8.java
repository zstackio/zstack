package org.zstack.test.aop;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 */
public class TestAsyncBackup8 {
    CLogger logger = Utils.getLogger(TestAsyncBackup8.class);
    boolean success;
    ComponentLoader loader;
    CloudBus bus;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        bus = loader.getComponent(CloudBus.class);
    }

    @AsyncThread
    private void testMethod(final Completion completion) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "1";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "2";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "3";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        throw new RuntimeException("error");
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        throw new RuntimeException("on purpose");
                    }
                });
            }
        }).start();
    }

    @Test
    public void test() throws InterruptedException {
        testMethod(new Completion(null) {
            @Override
            public void success() {
            }

            @Override
            public void fail(ErrorCode errorCode) {
                success = true;
                logger.debug(errorCode.toString());
            }
        });

        TimeUnit.SECONDS.sleep(1);
        Assert.assertTrue(success);
    }

}
