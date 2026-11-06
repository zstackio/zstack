package org.zstack.core.thread;

import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by MaJin on 2020/5/25.
 */
public abstract class GroupedConsumeQueue<T> {
    private static final CLogger logger = Utils.getLogger(GroupedConsumeQueue.class);

    private Queue<T> itemPool = new LinkedBlockingQueue<>();
    private Map<String, DelayedQueue<T>> groupedQueue = new HashMap<>();

    private int maxDelayedTime = 1;
    private final Timer timer = new Timer("GroupedConsumeQueue", true);
    private boolean started;

    public GroupedConsumeQueue(int maxDelayedTime) {
        this.maxDelayedTime = maxDelayedTime;
    }

    public void offer(T item) {
        if (maxDelayedTime <= 0) {
            consume(Collections.singletonList(item));
            return;
        }

        itemPool.offer(item);
    }

    public synchronized void start() {
        if (started) {
            return;
        }

        java.util.TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    collectItems();
                } catch (Throwable t) {
                    logger.warn("failed to collect grouped consume queue items", t);
                }
            }
        };

        timer.schedule(timerTask, 1000, 1000);
        started = true;
    }

    public void setMaxDelayedTime(int maxDelayedTime) {
        this.maxDelayedTime = maxDelayedTime;
    }

    protected abstract void consume(List<T> groupedItems);

    protected abstract String getGroupId(T item);

    static class DelayedQueue<T> {
        List<T> items = new ArrayList<>();
        private int delayedTime = 0;

        void delay() {
            ++delayedTime;
        }

        int getDelayedTime() {
            return delayedTime;
        }
    }

    private void collectItems() {
        T item;
        while ((item = itemPool.poll()) != null) {
            groupedQueue.computeIfAbsent(getGroupId(item), k -> new DelayedQueue<>()).items.add(item);
        }

        for (Iterator<DelayedQueue<T>> it = groupedQueue.values().iterator(); it.hasNext();) {
            DelayedQueue<T> queue = it.next();
            queue.delay();
            if (queue.getDelayedTime() >= maxDelayedTime) {
                consume(queue.items);
                it.remove();
            }
        }
    }
}
