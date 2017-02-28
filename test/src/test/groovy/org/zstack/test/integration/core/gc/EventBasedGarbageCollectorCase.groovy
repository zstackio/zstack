package org.zstack.test.integration.core.gc

import org.zstack.core.Platform
import org.zstack.core.cloudbus.EventFacade
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.errorcode.ErrorFacade
import org.zstack.core.groovy.gc.EventBasedGarbageCollector
import org.zstack.core.groovy.gc.GC
import org.zstack.core.gc.GCGlobalConfig
import org.zstack.core.gc.GCStatus
import org.zstack.core.groovy.gc.GarbageCollectorManagerImpl
import org.zstack.core.gc.GarbageCollectorVO
import org.zstack.testlib.SubCase

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Created by xing5 on 2017/3/1.
 */
class EventBasedGarbageCollectorCase extends SubCase {
    static final String EVENT_PATH = "/test/gc"
    static final String EVENT_PATH2 = "/test/gc2"
    static final String EVENT_PATH3 = "/test/gc3"


    DatabaseFacade dbf
    EventFacade evtf
    ErrorFacade errf
    GarbageCollectorManagerImpl gcMgr

    static class Context {
        String text
    }

    class EventBasedGC1 extends EventBasedGarbageCollector {
        Closure testLogic

        @Override
        protected void setup() {
            onEvent(EVENT_PATH, { testLogic() })
        }

        @Override
        protected void triggerNow() {
        }

        void done() {
            success()
        }

        void error() {
            fail(errf.stringToOperationError("on purpose"))
        }

        void drop() {
            cancel()
        }
    }

    class EventBasedGCTwoEventsTriggered extends EventBasedGarbageCollector {
        Closure testLogic

        @Override
        protected void setup() {
            onEvent(EVENT_PATH2, { testLogic() })
            onEvent(EVENT_PATH3, { testLogic() })
        }

        @Override
        protected void triggerNow() {

        }

        void done() {
            success()
        }

        void error() {
            fail(errf.stringToOperationError("on purpose"))
        }

        void drop() {
            cancel()
        }
    }

    static Closure<EventBasedGCInDbBehavior> testLogicForJobLoadedFromDb

    static enum EventBasedGCInDbBehavior {
        SUCCESS,
        FAIL,
        CANCEL
    }

    static class EventBasedGCInDb extends EventBasedGarbageCollector {
        @GC
        String name
        @GC
        String description
        @GC
        Context context

        @Override
        protected void setup() {
            onEvent(EVENT_PATH) {
                EventBasedGCInDbBehavior ret = testLogicForJobLoadedFromDb(this)

                if (ret == EventBasedGCInDbBehavior.SUCCESS) {
                    success()
                } else if (ret == EventBasedGCInDbBehavior.FAIL) {
                    fail(bean(ErrorFacade.class).stringToOperationError("on purpose"))
                } else if (ret == EventBasedGCInDbBehavior.CANCEL) {
                    cancel()
                } else {
                    assert false: "unknown behavior $ret"
                }
            }
        }

        @Override
        protected void triggerNow() {

        }

        void saveToDatabase() {
            saveToDb()
        }
    }

    static Closure<EventBasedGCInDbBehavior> testTriggerNowForJobLoadedFromDb

    static class EventBasedGCInDbTriggerNow extends EventBasedGarbageCollector {
        @GC
        String name
        @GC
        String description
        @GC
        Context context

        @Override
        protected void setup() {
            onEvent(EVENT_PATH) {
                triggerNow()
            }
        }

        @Override
        protected void triggerNow() {
            EventBasedGCInDbBehavior ret = testTriggerNowForJobLoadedFromDb(this)

            if (ret == EventBasedGCInDbBehavior.SUCCESS) {
                success()
            } else if (ret == EventBasedGCInDbBehavior.FAIL) {
                fail(bean(ErrorFacade.class).stringToOperationError("on purpose"))
            } else if (ret == EventBasedGCInDbBehavior.CANCEL) {
                cancel()
            } else {
                assert false: "unknown behavior $ret"
            }
        }

        void saveToDatabase() {
            saveToDb()
        }
    }

    @Override
    void setup() {
        INCLUDE_CORE_SERVICES = false
        //NEED_WEB_SERVER = false
    }

    @Override
    void environment() {
    }

    void testEventBasedGCSuccess() {
        int count = 0

        CountDownLatch latch = new CountDownLatch(1)

        def gc = new EventBasedGC1()
        gc.NAME = "testEventBasedGCSuccess"
        gc.testLogic = {
            count ++
            gc.done()
            latch.countDown()
        }
        gc.submit()

        GarbageCollectorVO vo = dbf.findById(gc.id, GarbageCollectorVO.class)
        assert vo != null
        assert vo.runnerClass == gc.class.name
        assert vo.context != null
        assert vo.status == GCStatus.Idle
        assert vo.managementNodeUuid == Platform.getManagementServerId()

        // trigger the GC
        evtf.fire(EVENT_PATH, "trigger it")
        latch.await(10, TimeUnit.SECONDS)

        assert count == 1
        vo = dbf.findById(gc.id, GarbageCollectorVO.class)
        assert vo == null

        // trigger again, confirm the event is no longer hooked
        evtf.fire(EVENT_PATH, "trigger it")
        TimeUnit.SECONDS.sleep(1)
        assert count == 1
    }

    void testEventBasedGCFailure() {
        CountDownLatch latch = new CountDownLatch(1)

        def gc = new EventBasedGC1()
        gc.testLogic = {
            gc.error()
            latch.countDown()
        }
        gc.NAME = "testEventBasedGCFailure"
        gc.submit()

        // trigger the GC
        evtf.fire(EVENT_PATH, "trigger it")
        latch.await(10, TimeUnit.SECONDS)

        GarbageCollectorVO vo = dbf.findById(gc.id, GarbageCollectorVO.class)
        assert vo.status == GCStatus.Idle

        // confirm re-run can success
        latch = new CountDownLatch(1)
        boolean s = false
        gc.testLogic = {
            s = true
            gc.done()
            latch.countDown()
        }

        evtf.fire(EVENT_PATH, "trigger it")
        latch.await(10, TimeUnit.SECONDS)
        vo = dbf.findById(gc.id, GarbageCollectorVO.class)
        assert vo == null
        assert s
    }

    void testEventBasedGCCancel() {
        CountDownLatch latch = new CountDownLatch(1)

        int count = 0
        def gc = new EventBasedGC1()
        gc.NAME = "testEventBasedGCCancel"
        gc.testLogic = {
            count ++
            gc.drop()
            latch.countDown()
        }
        gc.submit()

        // trigger the GC
        evtf.fire(EVENT_PATH, "trigger it")
        latch.await(10, TimeUnit.SECONDS)
        assert dbf.findById(gc.id, GarbageCollectorVO.class) == null
        assert count == 1

        // trigger again, confirm the event is no longer hooked
        evtf.fire(EVENT_PATH, "trigger it")
        TimeUnit.SECONDS.sleep(1)
        assert count == 1
    }

    void testEventBasedGCConcurrent() {
        CountDownLatch latch = new CountDownLatch(1)

        int count = 0
        def gc = new EventBasedGC1()

        gc.testLogic = {
            count ++
            gc.done()
            latch.countDown()
        }
        gc.NAME = "testEventBasedGCConcurrent"
        gc.submit()

        // trigger multiple times
        Thread.start {
            evtf.fire(EVENT_PATH, "trigger it")
        }
        Thread.start {
            evtf.fire(EVENT_PATH, "trigger it")
        }
        Thread.start {
            evtf.fire(EVENT_PATH, "trigger it")
        }

        latch.await(10, TimeUnit.SECONDS)
        // confirm the trigger only runs once
        assert count == 1
        assert dbf.findById(gc.id, GarbageCollectorVO.class) == null
    }

    void testEventBasedGCExceptionInTrigger() {
        CountDownLatch latch = new CountDownLatch(1)

        def gc = new EventBasedGC1()
        gc.testLogic = {
            try {
                throw new Exception("on purpose")
            } finally {
                latch.countDown()
            }
        }
        gc.NAME = "testEventBasedGCExceptionInTrigger"
        gc.submit()

        evtf.fire(EVENT_PATH, "trigger it")

        // wait for EventBasedGarbageCollector.fail() called
        TimeUnit.SECONDS.sleep(2)

        latch.await(10, TimeUnit.SECONDS)
        GarbageCollectorVO vo = dbf.findById(gc.id, GarbageCollectorVO.class)
        assert vo.status == GCStatus.Idle
        dbf.remove(vo)
    }

    void testTwoEventsTriggeredGC() {
        CountDownLatch latch = new CountDownLatch(2)

        int count = 0
        def gc = new EventBasedGCTwoEventsTriggered()

        gc.testLogic = {
            count ++
            if (count == 1) {
                gc.error()
            } else {
                gc.done()
            }
            latch.countDown()
        }
        gc.NAME = "testTwoEventsTriggeredGC"
        gc.submit()

        // use two different events to trigger the GC
        evtf.fire(EVENT_PATH2, "trigger it")
        TimeUnit.SECONDS.sleep(1)
        evtf.fire(EVENT_PATH3, "trigger it")

        latch.await(10, TimeUnit.SECONDS)
        assert count == 2
        assert dbf.findById(gc.id, GarbageCollectorVO.class) == null

        // trigger again, confirm the count is not increased, which
        // means the trigger is no longer hooked on the events
        evtf.fire(EVENT_PATH2, "trigger it")
        TimeUnit.SECONDS.sleep(1)
        evtf.fire(EVENT_PATH3, "trigger it")
        assert count == 2
    }

    void testLoadedOrphanJobCancel() {
        // create GC job just in the database
        def gc = new EventBasedGCInDb()
        gc.name = "test"
        gc.NAME = "testLoadedOrphanJobSuccess"
        gc.description = "description"
        gc.context = new Context()
        gc.context.text = "something"
        gc.saveToDatabase()

        // make the Job as an orphan
        GarbageCollectorVO vo = dbf.findById(gc.id, GarbageCollectorVO.class)
        vo.setManagementNodeUuid(null)
        dbf.update(vo)

        testLogicForJobLoadedFromDb = { return EventBasedGCInDbBehavior.CANCEL }

        // load orphan jobs
        gcMgr.managementNodeReady()
        evtf.fire(EVENT_PATH, "trigger it")
        TimeUnit.SECONDS.sleep(1)

        assert null == dbf.findById(gc.id, GarbageCollectorVO.class)
    }

    void testLoadedOrphanJobFailure() {
        // create GC job just in the database
        def gc = new EventBasedGCInDb()
        gc.name = "test"
        gc.NAME = "testLoadedOrphanJobSuccess"
        gc.description = "description"
        gc.context = new Context()
        gc.context.text = "something"
        gc.saveToDatabase()

        // make the Job as an orphan
        GarbageCollectorVO vo = dbf.findById(gc.id, GarbageCollectorVO.class)
        vo.setManagementNodeUuid(null)
        dbf.update(vo)

        testLogicForJobLoadedFromDb = { return EventBasedGCInDbBehavior.FAIL }

        // load orphan jobs
        gcMgr.managementNodeReady()
        evtf.fire(EVENT_PATH, "trigger it")
        TimeUnit.SECONDS.sleep(1)

        vo = dbf.findById(gc.id, GarbageCollectorVO.class)
        assert vo.status == GCStatus.Idle
        dbf.remove(vo)
    }

    void testLoadedOrphanJobTriggerNow() {
        // create GC job just in the database
        def gc = new EventBasedGCInDbTriggerNow()
        gc.name = "test"
        gc.NAME = "testLoadedOrphanJobTriggerNow"
        gc.description = "description"
        gc.context = new Context()
        gc.context.text = "something"
        gc.saveToDatabase()

        // make the Job as an orphan
        GarbageCollectorVO vo = dbf.findById(gc.id, GarbageCollectorVO.class)
        vo.setManagementNodeUuid(null)
        dbf.update(vo)

        boolean called = false
        testTriggerNowForJobLoadedFromDb = {
            called = true
            return EventBasedGCInDbBehavior.SUCCESS
        }

        // load orphan jobs
        gcMgr.managementNodeReady()

        TimeUnit.SECONDS.sleep(1)
        assert called
        assert dbf.findById(gc.id, GarbageCollectorVO.class) == null
    }

    void testLoadedOrphanJobSuccess() {
        // create GC job just in the database
        def gc = new EventBasedGCInDb()
        gc.name = "test"
        gc.NAME = "testLoadedOrphanJobSuccess"
        gc.description = "description"
        gc.context = new Context()
        gc.context.text = "something"
        gc.saveToDatabase()

        // make the Job as an orphan
        GarbageCollectorVO vo = dbf.findById(gc.id, GarbageCollectorVO.class)
        vo.setManagementNodeUuid(null)
        dbf.update(vo)

        int count = 0
        CountDownLatch latch = new CountDownLatch(1)

        String name = null
        String description = null
        Context ctx = null
        Long loadedJobId = null

        testLogicForJobLoadedFromDb = { EventBasedGCInDb g ->
            name = g.name
            description = g.description
            ctx = g.context
            loadedJobId = g.id

            count ++
            latch.countDown()
            return EventBasedGCInDbBehavior.SUCCESS
        }

        // load orphan jobs
        gcMgr.managementNodeReady()

        evtf.fire(EVENT_PATH, "trigger it")
        latch.await(10, TimeUnit.SECONDS)
        assert count == 1
        assert name == gc.name
        assert description == gc.description
        assert ctx != null
        assert ctx.text == gc.context.text

        // wait 1s for the job doing success()
        TimeUnit.SECONDS.sleep(1)
        vo = dbf.findById(loadedJobId, GarbageCollectorVO.class)
        assert vo == null
    }

    void testLoadedOrphanJobScan() {
        // create GC job just in the database
        def gc = new EventBasedGCInDb()
        gc.name = "test"
        gc.NAME = "testLoadedOrphanJobSuccess"
        gc.description = "description"
        gc.context = new Context()
        gc.context.text = "something"
        gc.saveToDatabase()

        // make the Job as an orphan
        GarbageCollectorVO vo = dbf.findById(gc.id, GarbageCollectorVO.class)
        vo.setManagementNodeUuid(null)
        dbf.update(vo)

        GCGlobalConfig.SCAN_ORPHAN_JOB_INTERVAL.updateValue(1)
        gcMgr.start()

        TimeUnit.SECONDS.sleep(2)

        testLogicForJobLoadedFromDb = { return EventBasedGCInDbBehavior.SUCCESS }

        evtf.fire(EVENT_PATH, "trigger it")

        TimeUnit.SECONDS.sleep(1)

        assert dbf.findById(gc.id, GarbageCollectorVO.class) == null
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)
        evtf = bean(EventFacade.class)
        errf = bean(ErrorFacade.class)
        gcMgr = bean(GarbageCollectorManagerImpl.class)

        testEventBasedGCSuccess()
        testEventBasedGCFailure()
        testEventBasedGCCancel()
        testEventBasedGCConcurrent()
        testEventBasedGCExceptionInTrigger()
        testTwoEventsTriggeredGC()
        testLoadedOrphanJobSuccess()
        testLoadedOrphanJobFailure()
        testLoadedOrphanJobCancel()
        testLoadedOrphanJobScan()
        testLoadedOrphanJobTriggerNow()
    }

    @Override
    void clean() {
        // nothing
    }
}
