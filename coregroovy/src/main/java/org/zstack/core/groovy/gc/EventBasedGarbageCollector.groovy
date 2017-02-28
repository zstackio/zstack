package org.zstack.core.groovy.gc

import org.zstack.core.cloudbus.EventCallback
import org.zstack.core.errorcode.ErrorFacade
import org.zstack.core.gc.GCStatus
import org.zstack.core.gc.GarbageCollectorVO
import org.zstack.core.thread.AsyncThread

/**
 * Created by xing5 on 2017/3/1.
 */
abstract class EventBasedGarbageCollector extends GarbageCollector {
    void load(GarbageCollectorVO vo) {
        loadFromVO(vo)
        setup()
        installTriggers()
        logger.debug("[GC] loaded a job[name:$NAME, id:$id]")

        Thread.start {
            try {
                executedTimes ++
                triggerNow()
            } catch (Throwable t) {
                logger.warn("[GC] unhandled exception happened when calling triggerNow() of a GC job[name:$NAME, id:$id]", t)
            }
        }
    }

    Map<String, Closure> triggers = [:]

    protected void onEvent(String path, Closure c) {
        triggers[path] = { data, tokens ->
            GarbageCollectorVO vo = dbf.findById(id, GarbageCollectorVO.class)

            if (vo == null) {
                logger.warn("[GC] cannot find a job[name:$NAME, id:$id], assume it's deleted")
                cancel()
                return
            }

            if (!lock()) {
                logger.debug("[GC] the job[name:$NAME, id:$id] is being executed by another trigger, skip this event[$path]")
                return
            }

            logger.debug("[GC] the job[name:$NAME, id:$id] is triggered by an event[$path]")
            try {
                vo.setStatus(GCStatus.Processing)
                dbf.update(vo)

                executedTimes ++
                if (c.maximumNumberOfParameters <= 1) {
                    c(data)
                } else {
                    c(data, tokens)
                }
            } catch (Throwable t) {
                logger.warn("[GC] unhandled exception happened when running a GC job[name:$NAME, id:$id]", t)
                ErrorFacade errf = bean(ErrorFacade.class)
                fail(errf.stringToInternalError(t.message))
            }
        }
    }

    protected abstract void setup()
    protected abstract void triggerNow()

    final void installTriggers() {
        setup()
        assert !triggers.isEmpty(): "${this.class} doesn't call onEvent() in the setup() to install any triggers"

        List<EventCallback> callbacks = []

        triggers.each { String path, Closure trigger ->
            def callback = new EventCallback() {
                @Override
                @AsyncThread
                protected void run(Map tokens, Object data) {
                    trigger(data, tokens)
                }
            }

            callbacks.add(callback)

            evtf.onLocal(path, callback)
        }

        canceller = {
            callbacks.each { evtf.off(it) }
        }
    }

    final void submit() {
        saveToDb()
        installTriggers()
    }
}
