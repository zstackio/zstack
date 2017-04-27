package org.zstack.test.integration.core.gc

import org.zstack.core.Platform
import org.zstack.core.cloudbus.EventFacade
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.SQL
import org.zstack.core.errorcode.ErrorFacade
import org.zstack.core.gc.*
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

    String adminSessionUuid

    static class Context {
        String text
    }

    class EventBasedGC1 extends EventBasedGarbageCollector {
        Closure trigger = { true }
        Closure testLogic

        @Override
        protected void setup() {
            onEvent(EVENT_PATH, { token, data ->
                return trigger()
            })
        }


        @Override
        protected void triggerNow(GCCompletion completion) {
            testLogic(completion)
        }
    }

    class EventBasedGCTwoEventsTriggered extends EventBasedGarbageCollector {
        Closure trigger = { true }
        Closure testLogic

        @Override
        protected void setup() {
            onEvent(EVENT_PATH2, { tokens, data ->
                trigger()
            })

            onEvent(EVENT_PATH3, { tokens, data ->
                trigger()
            })
        }

        @Override
        protected void triggerNow(GCCompletion completion) {
            testLogic(completion)
        }
    }

    static enum EventBasedGCInDbBehavior {
        SUCCESS,
        FAIL,
        CANCEL
    }

    static class EventBasedGCInDb extends EventBasedGarbageCollector {
        Closure trigger = { true }

        Closure<EventBasedGCInDbBehavior> testLogicForJobLoadedFromDb

        @GC
        String name
        @GC
        String description
        @GC
        Context context

        @Override
        protected void setup() {
            onEvent(EVENT_PATH) { tokens, data ->
                return trigger()
            }
        }

        void saveToDatabase() {
            saveToDb()
        }

        @Override
        protected void triggerNow(GCCompletion completion) {

            EventBasedGCInDbBehavior ret = testLogicForJobLoadedFromDb(this)

            if (ret == EventBasedGCInDbBehavior.SUCCESS) {
                completion.success()
            } else if (ret == EventBasedGCInDbBehavior.FAIL) {
                completion.fail(errf.stringToOperationError("on purpose"))
            } else if (ret == EventBasedGCInDbBehavior.CANCEL) {
                completion.cancel()
            } else {
                assert false: "unknown behavior $ret"
            }
        }
    }

    static class EventBasedGCInDbTriggerNow extends EventBasedGarbageCollector {
        Closure trigger = { true }

        Closure<EventBasedGCInDbBehavior> testTriggerNowForJobLoadedFromDb

        @GC
        String name
        @GC
        String description
        @GC
        Context context

        @Override
        protected void setup() {
            onEvent(EVENT_PATH) { tokens, data ->
                return trigger()
            }
        }

        void saveToDatabase() {
            saveToDb()
        }

        @Override
        protected void triggerNow(GCCompletion completion) {
            assert null != testTriggerNowForJobLoadedFromDb

            EventBasedGCInDbBehavior ret = testTriggerNowForJobLoadedFromDb(this)

            if (ret == EventBasedGCInDbBehavior.SUCCESS) {
                completion.success()
            } else if (ret == EventBasedGCInDbBehavior.FAIL) {
                completion.fail(errf.stringToOperationError("on purpose"))
            } else if (ret == EventBasedGCInDbBehavior.CANCEL) {
                completion.cancel()
            } else {
                assert false: "unknown behavior $ret"
            }
        }
    }

    @Override
    void setup() {
        INCLUDE_CORE_SERVICES = false
    }

    @Override
    void environment() {
        adminSessionUuid = loginAsAdmin().uuid
    }

    void testEventBasedGCSuccess() {
        int count = 0

        CountDownLatch latch = new CountDownLatch(1)

        def gc = new EventBasedGC1()
        gc.NAME = "testEventBasedGCSuccess"
        gc.testLogic = { GCCompletion completion ->
            count ++
            completion.success()
            latch.countDown()
        }
        gc.submit()

        GarbageCollectorVO vo = dbf.findByUuid(gc.uuid, GarbageCollectorVO.class)
        assert vo != null
        assert vo.runnerClass == gc.class.name
        assert vo.context != null
        assert vo.status == GCStatus.Idle
        assert vo.managementNodeUuid == Platform.getManagementServerId()

        // trigger the GC
        evtf.fire(EVENT_PATH, "trigger it")
        latch.await(10, TimeUnit.SECONDS)

        assert count == 1
        assert dbFindByUuid(gc.uuid, GarbageCollectorVO.class).status == GCStatus.Done

        // trigger again, confirm the event is no longer hooked
        evtf.fire(EVENT_PATH, "trigger it")

        assert retryInSecs {
            return {
                assert count == 1
            }
        }
    }

    void testEventBasedGCFailure() {
        CountDownLatch latch = new CountDownLatch(1)

        def gc = new EventBasedGC1()
        gc.testLogic = { GCCompletion completion ->
            completion.fail(errf.stringToOperationError("testEventBasedGCFailure"))
            latch.countDown()
        }
        gc.NAME = "testEventBasedGCFailure"
        gc.submit()

        // trigger the GC
        evtf.fire(EVENT_PATH, "trigger it")
        latch.await(10, TimeUnit.SECONDS)

        GarbageCollectorVO vo = dbf.findByUuid(gc.uuid, GarbageCollectorVO.class)
        assert vo.status == GCStatus.Idle

        // confirm re-run can success
        latch = new CountDownLatch(1)
        boolean s = false
        gc.testLogic = { GCCompletion completion ->
            s = true
            completion.success()
            latch.countDown()
        }

        evtf.fire(EVENT_PATH, "trigger it")
        latch.await(10, TimeUnit.SECONDS)
        assert dbFindByUuid(gc.uuid, GarbageCollectorVO.class).status == GCStatus.Done
        assert s
    }

    void testEventBasedGCCancel() {
        CountDownLatch latch = new CountDownLatch(1)

        int count = 0
        def gc = new EventBasedGC1()
        gc.NAME = "testEventBasedGCCancel"
        gc.testLogic = { GCCompletion completion ->
            count ++
            completion.cancel()
            latch.countDown()
        }
        gc.submit()

        // trigger the GC
        evtf.fire(EVENT_PATH, "trigger it")
        latch.await(10, TimeUnit.SECONDS)
        assert dbFindByUuid(gc.uuid, GarbageCollectorVO.class).status == GCStatus.Done
        assert count == 1

        // trigger again, confirm the event is no longer hooked
        evtf.fire(EVENT_PATH, "trigger it")

        assert retryInSecs {
            return {
                assert count == 1
            }
        }

    }

    void testEventBasedGCConcurrent() {
        CountDownLatch latch = new CountDownLatch(1)

        int count = 0
        def gc = new EventBasedGC1()

        gc.testLogic = { GCCompletion completion ->
            count ++
            completion.cancel()
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
        assert dbFindByUuid(gc.uuid, GarbageCollectorVO.class).status == GCStatus.Done
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
        latch.await(10, TimeUnit.SECONDS)

        assert retryInSecs {
            return {
                GarbageCollectorVO vo = dbf.findByUuid(gc.uuid, GarbageCollectorVO.class)
                assert vo.status == GCStatus.Idle
                dbf.remove(vo)
            }
        }
    }

    void testTwoEventsTriggeredGC() {
        CountDownLatch latch = new CountDownLatch(2)

        int count = 0
        def gc = new EventBasedGCTwoEventsTriggered()

        gc.testLogic = { GCCompletion completion ->
            count ++
            if (count == 1) {
                completion.fail(errf.stringToOperationError("testTwoEventsTriggeredGC"))
            } else {
                completion.success()
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
        assert dbFindByUuid(gc.uuid, GarbageCollectorVO.class).status == GCStatus.Done

        // trigger again, confirm the count is not increased, which
        // means the trigger is no longer hooked on the events
        evtf.fire(EVENT_PATH2, "trigger it")
        TimeUnit.SECONDS.sleep(1)
        evtf.fire(EVENT_PATH3, "trigger it")

        assert retryInSecs {
            return {
                assert count == 2
            }
        }
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
        GarbageCollectorVO vo = dbf.findByUuid(gc.uuid, GarbageCollectorVO.class)
        vo.setManagementNodeUuid(null)
        dbf.update(vo)

        gc.testLogicForJobLoadedFromDb = { return EventBasedGCInDbBehavior.CANCEL }

        // load orphan jobs
        gcMgr.managementNodeReady()
        evtf.fire(EVENT_PATH, "trigger it")

        assert retryInSecs {
            return {
                assert dbFindByUuid(gc.uuid, GarbageCollectorVO.class).status == GCStatus.Done
            }
        }
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
        GarbageCollectorVO vo = dbf.findByUuid(gc.uuid, GarbageCollectorVO.class)
        vo.setManagementNodeUuid(null)
        dbf.update(vo)

        gc.testLogicForJobLoadedFromDb = { return EventBasedGCInDbBehavior.FAIL }

        // load orphan jobs
        gcMgr.managementNodeReady()
        evtf.fire(EVENT_PATH, "trigger it")

        assert retryInSecs {
            return {
                vo = dbf.findByUuid(gc.uuid, GarbageCollectorVO.class)
                assert vo.status == GCStatus.Idle
                dbf.remove(vo)
            }
        }
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
        GarbageCollectorVO vo = dbf.findByUuid(gc.uuid, GarbageCollectorVO.class)
        vo.setManagementNodeUuid(null)
        dbf.update(vo)

        boolean called = false
        gc.testTriggerNowForJobLoadedFromDb = {
            called = true
            return EventBasedGCInDbBehavior.SUCCESS
        }

        // load orphan jobs
        gcMgr.managementNodeReady()

        assert retryInSecs {
            return {
                assert called
                assert dbFindByUuid(gc.uuid, GarbageCollectorVO.class).status == GCStatus.Done
            }
        }
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
        GarbageCollectorVO vo = dbf.findByUuid(gc.uuid, GarbageCollectorVO.class)
        vo.setManagementNodeUuid(null)
        dbf.update(vo)

        int count = 0
        CountDownLatch latch = new CountDownLatch(1)

        String name = null
        String description = null
        Context ctx = null
        String loadedJobId = null

        gc.testLogicForJobLoadedFromDb = { EventBasedGCInDb g ->
            name = g.name
            description = g.description
            ctx = g.context
            loadedJobId = g.uuid

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

        assert retryInSecs {
            return {
                assert dbFindByUuid(gc.uuid, GarbageCollectorVO.class).status == GCStatus.Done
            }
        }
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
        GarbageCollectorVO vo = dbf.findByUuid(gc.uuid, GarbageCollectorVO.class)
        vo.setManagementNodeUuid(null)
        dbf.update(vo)

        GCGlobalConfig.SCAN_ORPHAN_JOB_INTERVAL.updateValue(1)
        gcMgr.start()

        TimeUnit.SECONDS.sleep(2)

        gc.testLogicForJobLoadedFromDb = { return EventBasedGCInDbBehavior.SUCCESS }

        evtf.fire(EVENT_PATH, "trigger it")

        assert retryInSecs {
            return {
                assert dbFindByUuid(gc.uuid, GarbageCollectorVO.class).status == GCStatus.Done
            }
        }
    }

    void testEventBasedGCCancelByApi() {
        int count = 0
        def gc = new EventBasedGC1()
        gc.NAME = "testEventBasedGCCancel"
        gc.testLogic = { GCCompletion completion ->
            count ++
            completion.cancel()
        }
        gc.submit()

        deleteGCJob {
            uuid = gc.uuid
            sessionId = adminSessionUuid
        }

        // trigger the GC
        evtf.fire(EVENT_PATH, "trigger it")

        TimeUnit.SECONDS.sleep(1)

        assert retryInSecs {
            return {
                // trigger again, confirm the event is no longer hooked
                assert !dbIsExists(gc.uuid, GarbageCollectorVO.class)
                assert count == 0
            }
        }
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
        testEventBasedGCCancelByApi()
    }

    @Override
    void clean() {
        SQL.New(GarbageCollectorVO.class).delete()
    }
}
