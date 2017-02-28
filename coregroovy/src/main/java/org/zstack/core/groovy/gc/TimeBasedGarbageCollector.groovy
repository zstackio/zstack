package org.zstack.core.groovy.gc

import org.zstack.core.gc.GarbageCollectorVO
import org.zstack.header.errorcode.ErrorCode

import java.util.concurrent.TimeUnit

/**
 * Created by xing5 on 2017/3/2.
 */
abstract class TimeBasedGarbageCollector extends GarbageCollector {
    @GC
    volatile Long NEXT_TIME
    @GC
    volatile TimeUnit NEXT_TIME_UNIT

    protected abstract void triggerNow()

    protected TimerTask currentTimer

    TimeBasedGarbageCollector() {
        canceller = {
            // do nothing
        }
    }

    protected void setupTimer() {
        logger.debug("[GC] schedule a GC job[name:$NAME, id:$id] to run after $NEXT_TIME $NEXT_TIME_UNIT")

        currentTimer = new Timer().runAfter(NEXT_TIME_UNIT.toMillis(NEXT_TIME) as int) {
            try {
                executedTimes ++
                triggerNow()
            } catch (Throwable t) {
                logger.warn("[GC] unhandled exception happened when calling triggerNow() of a GC job[name:$NAME, id:$id]", t)
                fail(errf.stringToOperationError(t.message))
            }
        }
    }

    @Override
    protected void cancel() {
        if (currentTimer != null)  {
            currentTimer.cancel()
        }

        super.cancel()
    }

    @Override
    protected void fail(ErrorCode err) {
        super.fail(err)
        setupTimer()
    }

    void load(GarbageCollectorVO vo) {
        loadFromVO(vo)
        setupTimer()
    }

    final void submit(Long next, TimeUnit unit) {
        NEXT_TIME_UNIT = unit
        NEXT_TIME = next

        saveToDb()
        setupTimer()
    }
}
