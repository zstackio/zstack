package org.zstack.test.core.db;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.GLock;
import org.zstack.core.thread.AsyncThread;
import org.zstack.test.BeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestGlobalDbLock {
    ComponentLoader loader;
    DatabaseFacade dbf;
    CLogger logger = Utils.getLogger(TestGlobalDbLock.class);
    String lockName = "TestDBLock.lock";
    String lockName2 = "TestDBLock.lock2";
    CountDownLatch latch = new CountDownLatch(2);
    boolean lock1Success = false;
    boolean lock2Success = false;
    int num = 3000;

    @Before
    public void setUp() throws Exception {
        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @AsyncThread
    void lock1() {
        int lock1Count = 0;
        for (int i = 0; i < num; i++) {
            GLock lock1 = new GLock(lockName, 10);
            lock1.lock();
            try {
                logger.debug("lock thread 1: Having locked: " + i + " times");
                GLock lock2 = new GLock(lockName2, 10);
                lock2.lock();
                try {
                    lock1Count++;
                } finally {
                    lock2.unlock();
                }
            } finally {
                lock1.unlock();
            }
        }
        if (lock1Count == num) {
            lock1Success = true;
        }
        latch.countDown();
    }

    @AsyncThread
    void lock2() {
        int lock2Count = 0;
        for (int i = 0; i < num; i++) {
            GLock lock1 = new GLock(lockName, 10);
            lock1.lock();
            try {
                GLock lock2 = new GLock(lockName2, 10);
                lock2.lock();
                logger.debug("lock thread 2: Having locked: " + i + " times");
                try {
                    lock2Count++;
                } finally {
                    lock2.unlock();
                }
            } finally {
                lock1.unlock();
            }
        }
        if (lock2Count == num) {
            lock2Success = true;
        }
        latch.countDown();
    }

    @Test
    public void test() throws InterruptedException {
        lock1();
        lock2();
        latch.await(60, TimeUnit.SECONDS);
        Assert.assertTrue(lock1Success);
        Assert.assertTrue(lock2Success);
    }
}
