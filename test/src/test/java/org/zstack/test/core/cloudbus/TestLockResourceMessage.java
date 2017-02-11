package org.zstack.test.core.cloudbus;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.AutoOffEventCallback;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusIN;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.Component;
import org.zstack.header.Service;
import org.zstack.header.message.*;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestLockResourceMessage {
    CLogger logger = Utils.getLogger(TestLockResourceMessage.class);
    ComponentLoader loader;
    CloudBusIN bus;
    ThreadFacade thdf;
    EventFacade evtf;
    CountDownLatch latch = new CountDownLatch(1);
    boolean isSuccess = false;
    boolean success = false;

    public static class TestLockMsg extends LockResourceMessage {
    }

    public static class TestLockReply extends LockResourceReply {
    }

    public static class TestMsg extends NeedReplyMessage {
    }

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        bus = loader.getComponent(CloudBusIN.class);
        thdf = loader.getComponent(ThreadFacade.class);
        evtf = loader.getComponent(EventFacade.class);
        ((Component) evtf).start();
    }

    @Test
    public void test() throws InterruptedException, ClassNotFoundException {
        final String serviceId = "Test";
        Service serv = new AbstractService() {
            String syncName = "test";

            @Override
            public void handleMessage(Message msg) {
                if (msg instanceof TestLockMsg) {
                    handle((TestLockMsg) msg);
                } else if (msg instanceof TestMsg) {
                    handle((TestMsg) msg);
                }
            }

            private void handle(final TestMsg msg) {
                thdf.chainSubmit(new ChainTask(msg) {
                    @Override
                    public String getSyncSignature() {
                        return syncName;
                    }

                    @Override
                    public void run(SyncTaskChain chain) {
                        MessageReply r = new MessageReply();
                        bus.reply(msg, r);
                        chain.next();
                    }

                    @Override
                    public String getName() {
                        return syncName;
                    }
                });
            }

            private void handle(final TestLockMsg msg) {
                thdf.chainSubmit(new ChainTask(msg) {
                    @Override
                    public String getSyncSignature() {
                        return syncName;
                    }

                    @Override
                    public void run(final SyncTaskChain chain) {
                        evtf.on(LockResourceMessage.UNLOCK_CANONICAL_EVENT_PATH, new AutoOffEventCallback() {
                            @Override
                            public boolean run(Map tokens, Object data) {
                                logger.debug(String.format("received unlock key: %s", data));
                                if (msg.getUnlockKey().equals(data)) {
                                    logger.debug("unlocked");
                                    chain.next();
                                    return true;
                                }
                                return false;
                            }
                        });
                        TestLockReply r = new TestLockReply();
                        bus.reply(msg, r);
                    }

                    @Override
                    public String getName() {
                        return syncName;
                    }
                });
            }

            @Override
            public String getId() {
                return bus.makeLocalServiceId(serviceId);
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

        String lockUuid = Platform.getUuid();
        TestLockMsg lockMsg = new TestLockMsg();
        lockMsg.setUnlockKey(lockUuid);
        bus.makeLocalServiceId(lockMsg, serviceId);
        bus.call(lockMsg);

        TestMsg tmsg = new TestMsg();
        bus.makeLocalServiceId(tmsg, serviceId);
        bus.send(tmsg, new CloudBusCallBack(null) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    success = true;
                }
            }
        });
        TimeUnit.SECONDS.sleep(1);
        Assert.assertFalse(success);

        evtf.fire(LockResourceMessage.UNLOCK_CANONICAL_EVENT_PATH, lockUuid);
        TimeUnit.SECONDS.sleep(1);
        Assert.assertTrue(success);
    }
}
