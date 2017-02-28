package org.zstack.test.integration.core.gc

import org.zstack.core.db.DatabaseFacade
import org.zstack.core.gc.GCGlobalConfig
import org.zstack.core.gc.GCStatus
import org.zstack.core.gc.GarbageCollectorVO
import org.zstack.core.groovy.gc.GarbageCollectorManagerImpl
import org.zstack.core.groovy.gc.TimeBasedGarbageCollector
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit

/**
 * Created by xing5 on 2017/3/2.
 */
class TimeBasedGarbageCollectorCase extends SubCase {
    DatabaseFacade dbf
    GarbageCollectorManagerImpl gcMgr

    static enum Behavior {
        SUCCESS,
        FAILURE,
        CANCEL
    }

    class TimeBasedGC1 extends TimeBasedGarbageCollector {
        Closure triggerNowLogic

        @Override
        protected void triggerNow() {
            def ret = triggerNowLogic()
            if (ret == Behavior.SUCCESS) {
                success()
            } else if (ret == Behavior.FAILURE) {
                fail(errf.stringToOperationError("failure"))
            } else if (ret == Behavior.CANCEL) {
                cancel()
            } else {
                assert false: "unknown behavior $ret"
            }
        }

        void doCancel() {
            cancel()
        }
    }

    static Closure<Behavior> triggerNowLogicInDb

    static class TimeBasedGCInDb extends TimeBasedGarbageCollector {

        @Override
        protected void triggerNow() {
            def ret = triggerNowLogicInDb()
            if (ret == Behavior.SUCCESS) {
                success()
            } else if (ret == Behavior.FAILURE) {
                fail(errf.stringToOperationError("on purpose"))
            } else if (ret == Behavior.CANCEL) {
                cancel()
            } else {
                assert false: "unknown behavior $ret"
            }
        }

        void save() {
            saveToDb()
        }
    }

    @Override
    void setup() {
    }

    @Override
    void environment() {

    }

    void testGCSuccess() {
        int count = 0

        def gc = new TimeBasedGC1()
        gc.NAME = "testGCSuccess"
        gc.triggerNowLogic = {
            count ++
            return Behavior.SUCCESS
        }
        gc.submit(500, TimeUnit.MILLISECONDS)

        TimeUnit.SECONDS.sleep(1)
        assert count == 1
        assert !dbIsExists(gc.id, GarbageCollectorVO.class)

        // confirm the GC is not called anymore
        TimeUnit.SECONDS.sleep(1)
        assert count == 1
    }

    void testGCFail() {
        int count = 0

        def gc = new TimeBasedGC1()
        gc.NAME = "testGCFail"
        gc.triggerNowLogic = {
            count ++
            return Behavior.FAILURE
        }
        gc.submit(500, TimeUnit.MILLISECONDS)

        TimeUnit.SECONDS.sleep(2)

        GarbageCollectorVO vo = dbFindById(gc.id, GarbageCollectorVO.class)
        assert vo != null
        assert count > 1
        gc.doCancel()
    }

    void testGCCancel() {
        boolean called = false

        def gc = new TimeBasedGC1()
        gc.NAME = "testGCCancel"
        gc.triggerNowLogic = {
            called = true
            return Behavior.CANCEL
        }
        gc.submit(500, TimeUnit.MILLISECONDS)

        TimeUnit.SECONDS.sleep(1)
        assert called
        assert !dbIsExists(gc.id, GarbageCollectorVO.class)
    }

    void testGCException() {
        int count = 0
        def gc = new TimeBasedGC1()
        gc.NAME = "testGCException"
        gc.triggerNowLogic = {
            count ++
            throw new Exception("on purpose")
        }
        gc.submit(500, TimeUnit.MILLISECONDS)

        TimeUnit.SECONDS.sleep(2)

        // confirm the job is still there
        GarbageCollectorVO vo = dbFindById(gc.id, GarbageCollectorVO.class)
        assert count > 1
        assert vo != null
        gc.doCancel()
    }


    void testGCLoadedFromDbSuccess() {
        boolean called = false
        def gc = new TimeBasedGCInDb()
        gc.NEXT_TIME = 500
        gc.NEXT_TIME_UNIT = TimeUnit.MILLISECONDS
        gc.NAME = "testGCLoadedFromDbSuccess"
        gc.save()

        GarbageCollectorVO vo = dbFindById(gc.id, GarbageCollectorVO.class)
        vo.setManagementNodeUuid(null)
        dbf.update(vo)

        triggerNowLogicInDb = {
            called = true
            return Behavior.SUCCESS
        }

        gcMgr.managementNodeReady()

        TimeUnit.SECONDS.sleep(1)
        assert called
        assert !dbIsExists(gc.id, GarbageCollectorVO.class)
    }

    void testGCLoadedFromDbFailure() {
        def gc = new TimeBasedGCInDb()
        gc.NEXT_TIME = 500
        gc.NAME = "testGCLoadedFromDbFailure"
        gc.NEXT_TIME_UNIT = TimeUnit.MILLISECONDS
        gc.save()

        GarbageCollectorVO vo = dbFindById(gc.id, GarbageCollectorVO.class)
        vo.setManagementNodeUuid(null)
        dbf.update(vo)

        triggerNowLogicInDb = {
            return Behavior.FAILURE
        }

        gcMgr.managementNodeReady()

        TimeUnit.SECONDS.sleep(1)
        vo = dbFindById(gc.id, GarbageCollectorVO.class)
        assert vo != null
        assert vo.status == GCStatus.Idle
    }

    void testGCLoadedFromDbCancel() {
        boolean called = false
        def gc = new TimeBasedGCInDb()
        gc.NAME = "testGCLoadedFromDbCancel"
        gc.NEXT_TIME = 500
        gc.NEXT_TIME_UNIT = TimeUnit.MILLISECONDS
        gc.save()

        GarbageCollectorVO vo = dbFindById(gc.id, GarbageCollectorVO.class)
        vo.setManagementNodeUuid(null)
        dbf.update(vo)

        triggerNowLogicInDb = {
            called = true
            return Behavior.CANCEL
        }

        gcMgr.managementNodeReady()

        TimeUnit.SECONDS.sleep(1)
        assert called
        assert !dbIsExists(gc.id, GarbageCollectorVO.class)
    }

    void testGCScanOrphan() {
        boolean called = false
        def gc = new TimeBasedGCInDb()
        gc.NEXT_TIME = 500
        gc.NEXT_TIME_UNIT = TimeUnit.MILLISECONDS
        gc.NAME = "testGCScanOrphan"
        gc.save()

        GarbageCollectorVO vo = dbFindById(gc.id, GarbageCollectorVO.class)
        vo.setManagementNodeUuid(null)
        dbf.update(vo)

        GCGlobalConfig.SCAN_ORPHAN_JOB_INTERVAL.updateValue(1)
        gcMgr.start()

        triggerNowLogicInDb = {
            called = true
            return Behavior.SUCCESS
        }

        TimeUnit.SECONDS.sleep(1)
        assert called
        assert !dbIsExists(gc.id, GarbageCollectorVO.class)
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)
        gcMgr = bean(GarbageCollectorManagerImpl.class)

        testGCSuccess()
        testGCFail()
        testGCCancel()
        testGCException()
        testGCLoadedFromDbSuccess()
        testGCLoadedFromDbCancel()
        testGCLoadedFromDbFailure()
        testGCScanOrphan()
    }

    @Override
    void clean() {
        /* nothing */
    }
}
