package org.zstack.test.integration.core.taskqueue

import org.zstack.core.thread.MergeQueue
import org.zstack.core.thread.ChainTask
import org.zstack.core.thread.SyncTaskChain
import org.zstack.core.thread.ThreadFacade
import org.zstack.testlib.SubCase
import org.mockito.Mockito

import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier

/**
 * Created by mingjian.deng on 2020/4/29.*/
class MergeQueueCase extends SubCase {
    @Override
    void clean() {

    }

    @Override
    void setup() {

    }

    @Override
    void environment() {

    }

    @Override
    void test() {
        test1()
        test2()
        test3()
        test4()
        testSubmitAfterCounterIsRemovedDuringLookup()
    }

    void testSubmitAfterCounterIsRemovedDuringLookup() {
        String name = "counter-removal-${UUID.randomUUID()}"
        def lookupStarted = new CountDownLatch(1)
        def counterRemoved = new CountDownLatch(1)
        def firstRead = new AtomicBoolean(true)
        def counters = new ConcurrentHashMap<String, AtomicInteger>() {
            @Override
            AtomicInteger get(Object key) {
                AtomicInteger snapshot = super.get(key)
                if (key == name && snapshot != null && firstRead.compareAndSet(true, false)) {
                    lookupStarted.countDown()
                    assert counterRemoved.await(5, TimeUnit.SECONDS)
                }
                return snapshot
            }
        }
        counters.put(name, new AtomicInteger(1))
        def submitted = new AtomicReference<ChainTask>()
        def facade = Mockito.mock(ThreadFacade.class)
        Mockito.doAnswer { invocation ->
            submitted.set(invocation.getArgument(0))
            return null
        }.when(facade).chainSubmit(Mockito.any(ChainTask.class))
        def executions = new AtomicInteger()
        def queue = new MergeQueue().addTask(name, { executions.incrementAndGet(); null } as Supplier<Void>)
        def facadeField = MergeQueue.getDeclaredField("thdf")
        facadeField.accessible = true
        facadeField.set(queue, facade)
        def counterField = MergeQueue.getDeclaredField("counter")
        counterField.accessible = true
        def original = counterField.get(null)
        def removal = new FutureTask<Void>({
            try {
                assert lookupStarted.await(5, TimeUnit.SECONDS)
                counters.computeIfPresent(name) { key, value ->
                    assert value.decrementAndGet() == 0
                    return null
                }
                return null
            } finally {
                counterRemoved.countDown()
            }
        } as Callable<Void>)
        Thread worker = Thread.start { removal.run() }
        try {
            counterField.set(null, counters)
            queue.run()
            assert submitted.get() != null : "A drained queue must accept the next task"
            submitted.get().run(Mockito.mock(SyncTaskChain.class))
            assert executions.get() == 1
            assert !counters.containsKey(name)
        } finally {
            counterRemoved.countDown()
            try {
                removal.get(5, TimeUnit.SECONDS)
                worker.join(5000)
                assert !worker.alive
            } finally {
                counterField.set(null, original)
            }
        }
    }

    void test1() {
        assertCoalescedExecutions(1, 1, 2)
    }

    void test2() {
        String name = "sequential-${UUID.randomUUID()}"
        def executions = new AtomicInteger()
        (1..10).each {
            new MergeQueue().addTask(name, { executions.incrementAndGet(); null } as Supplier<Void>).run()
            awaitQueue(name)
        }
        assert executions.get() == 10
    }

    void test3() {
        assertCoalescedExecutions(1, 2, 4)
    }

    void test4() {
        assertCoalescedExecutions(3, 1, 4)
    }

    private void assertCoalescedExecutions(int syncLevel, int queueCount, int expected) {
        def release = new CountDownLatch(1)
        def executions = new AtomicInteger()
        def names = (1..queueCount).collect { "coalesced-${UUID.randomUUID()}".toString() }
        def execution = {
            assert release.await(5, TimeUnit.SECONDS)
            executions.incrementAndGet()
            return null
        } as Supplier<Void>
        try {
            names.each { name ->
                (1..10).each {
                    new MergeQueue().addTask(name, execution).setSyncLevel(syncLevel).run()
                }
            }
        } finally {
            release.countDown()
        }
        names.each { awaitQueue(it) }
        assert executions.get() == expected
    }

    private void awaitQueue(String name) {
        def counterField = MergeQueue.getDeclaredField("counter")
        counterField.accessible = true
        Map counters = counterField.get(null)
        retryInSecs {
            assert !counters.containsKey(name) : "Queue ${name} did not drain"
        }
    }
}
