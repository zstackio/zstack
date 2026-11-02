package org.zstack.test.core.thread;

import org.junit.Assert;
import org.junit.Test;
import org.zstack.core.thread.GroupedConsumeQueue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TestGroupedConsumeQueue {
    private static class Queue extends GroupedConsumeQueue<String> {
        private final CountDownLatch consumed = new CountDownLatch(1);
        private final AtomicInteger consumeCount = new AtomicInteger();

        Queue(int maxDelayedTime) {
            super(maxDelayedTime);
        }

        @Override
        protected void consume(List<String> groupedItems) {
            consumeCount.incrementAndGet();
            consumed.countDown();
        }

        @Override
        protected String getGroupId(String item) {
            return item;
        }
    }

    @Test
    public void startIsIdempotent() throws InterruptedException {
        Queue queue = new Queue(2);

        queue.start();
        queue.start();
        Thread.sleep(1200);

        queue.offer("vm-progress");

        Assert.assertFalse(queue.consumed.await(1200, TimeUnit.MILLISECONDS));
        Assert.assertTrue(queue.consumed.await(1500, TimeUnit.MILLISECONDS));
        Assert.assertEquals(1, queue.consumeCount.get());
    }
}
