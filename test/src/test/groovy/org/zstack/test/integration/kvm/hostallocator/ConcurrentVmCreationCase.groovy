package org.zstack.test.integration.kvm.hostallocator

import org.zstack.core.thread.SyncTask
import org.zstack.core.thread.ThreadFacadeImpl
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class ConcurrentVmCreationCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.noVmThreeHostEnv()
    }

    @Override
    void test() {
        env.create {
            testSyncSubmit()
        }
    }

    void testSyncSubmit() {
        ThreadFacadeImpl thdf = bean(ThreadFacadeImpl.class)
        AtomicInteger counter = new AtomicInteger(10)

        def task = new SyncTask<Void>() {
            @Override
            String getSyncSignature() {
                return "sync-task-test"
            }

            @Override
            int getSyncLevel() {
                return 2
            }

            @Override
            String getName() {
                return "demo task"
            }

            @Override
            Void call() throws Exception {
                logger.info("task running")
                TimeUnit.SECONDS.sleep(1)
                counter.decrementAndGet()
                return null
            }
        }

        1.upto(10) {
            thdf.syncSubmit(task)
        }

        retryInSecs(8, 1) {
            assert counter.get() == 0
        }
    }
}
