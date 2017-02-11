package org.zstack.test.aop;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBusIN;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.AbstractService;
import org.zstack.header.Service;
import org.zstack.header.core.AsyncBackup;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

/**
 */
public class TestAsyncBackup2 {
    CLogger logger = Utils.getLogger(TestAsyncBackup2.class);
    boolean success;
    ComponentLoader loader;
    CloudBusIN bus;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        bus = loader.getComponent(CloudBusIN.class);
    }

    private void testMethod(final AsyncBackup completion) {
        new NoErrorCompletion(completion) {
            @Override
            @AsyncThread
            public void done() {
                throw new RuntimeException("on purpose");

            }
        }.done();
    }

    @Test
    public void test() throws InterruptedException {
        success = false;
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

    @Test
    public void test1() throws InterruptedException {
        success = false;
        testMethod(new NoErrorCompletion() {
            @Override
            public void done() {
                success = true;
            }
        });

        TimeUnit.SECONDS.sleep(1);
        Assert.assertTrue(success);
    }

    @Test
    public void test2() throws InterruptedException {
        success = false;
        testMethod(new ReturnValueCompletion(null) {
            @Override
            public void success(Object returnValue) {

            }

            @Override
            public void fail(ErrorCode errorCode) {
                success = true;
            }
        });

        TimeUnit.SECONDS.sleep(1);
        Assert.assertTrue(success);
    }

    @Test
    public void test3() throws InterruptedException {
        success = false;
        testMethod(new FlowTrigger() {
            @Override
            public void fail(ErrorCode errorCode) {
                success = true;
            }

            @Override
            public void next() {

            }

            @Override
            public void setError(ErrorCode error) {

            }
        });

        TimeUnit.SECONDS.sleep(1);
        Assert.assertTrue(success);
    }

    public static class TestMsg extends NeedReplyMessage {
    }

    @Test
    public void test4() throws InterruptedException {

        success = false;
        Service serv = new AbstractService() {
            @Override
            public void handleMessage(final Message msg) {
                new Completion(msg) {
                    @Override
                    @AsyncThread
                    public void success() {
                        throw new RuntimeException("on purpose");
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {

                    }
                }.success();
            }

            @Override
            public String getId() {
                return "Test1";
            }

            @Override
            public boolean start() {
                return true;
            }

            @Override
            public boolean stop() {
                return true;
            }
        };
        bus.registerService(serv);
        bus.activeService(serv);

        TestMsg msg = new TestMsg();
        msg.setServiceId("Test1");
        msg.setTimeout(500);
        MessageReply reply = bus.call(msg);

        Assert.assertFalse(reply.isSuccess());
    }
}
