package org.zstack.test.integration.core.gc

import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.SQL
import org.zstack.core.gc.GCCompletion
import org.zstack.core.gc.GCGlobalConfig
import org.zstack.core.gc.GCStatus
import org.zstack.core.gc.GarbageCollector
import org.zstack.core.gc.GarbageCollectorVO
import org.zstack.core.gc.GarbageCollectorManagerImpl
import org.zstack.core.gc.TimeBasedGarbageCollector
import org.zstack.sdk.GarbageCollectorInventory
import org.zstack.testlib.SubCase

import java.util.concurrent.TimeUnit

/**
 * Created by xing5 on 2017/3/2.
 */
class TimeBasedGarbageCollectorCase extends SubCase {
    DatabaseFacade dbf
    GarbageCollectorManagerImpl gcMgr
    String adminSessionUuid

    static enum Behavior {
        SUCCESS,
        FAILURE,
        CANCEL
    }

    class TimeBasedGC1 extends TimeBasedGarbageCollector {
        Closure triggerNowLogic

        void doCancel() {
            cancel()
        }

        @Override
        protected void triggerNow(GCCompletion completion) {
            def ret = triggerNowLogic()
            if (ret == Behavior.SUCCESS) {
                completion.success()
            } else if (ret == Behavior.FAILURE) {
                completion.fail(errf.stringToOperationError("failure"))
            } else if (ret == Behavior.CANCEL) {
                completion.cancel()
            } else {
                assert false: "unknown behavior $ret"
            }
        }
    }

    static Closure<Behavior> triggerNowLogicInDb

    static class TimeBasedGCInDb extends TimeBasedGarbageCollector {
        @Override
        protected void triggerNow(GCCompletion completion) {
            def ret = triggerNowLogicInDb()
            if (ret == Behavior.SUCCESS) {
                completion.success()
            } else if (ret == Behavior.FAILURE) {
                completion.fail(errf.stringToOperationError("failure"))
            } else if (ret == Behavior.CANCEL) {
                completion.cancel()
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
        adminSessionUuid = loginAsAdmin().uuid
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

        retryInSecs {
            assert count == 1
            assert dbFindByUuid(gc.uuid, GarbageCollectorVO.class).status == GCStatus.Done
        }

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

        retryInSecs {
            GarbageCollectorVO vo = dbFindByUuid(gc.uuid, GarbageCollectorVO.class)
            assert vo != null
            assert count > 1
        }

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


        retryInSecs {
            assert called
            assert dbFindByUuid(gc.uuid, GarbageCollectorVO.class).status == GCStatus.Done
        }
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

        retryInSecs {
            // confirm the job is still there
            GarbageCollectorVO vo = dbFindByUuid(gc.uuid, GarbageCollectorVO.class)
            assert count > 1
            assert vo != null
        }

        gc.doCancel()
    }


    void testGCLoadedFromDbSuccess() {
        boolean called = false
        def gc = new TimeBasedGCInDb()
        gc.NEXT_TIME = 500
        gc.NEXT_TIME_UNIT = TimeUnit.MILLISECONDS
        gc.NAME = "testGCLoadedFromDbSuccess"
        gc.save()

        GarbageCollectorVO vo = dbFindByUuid(gc.uuid, GarbageCollectorVO.class)
        vo.setManagementNodeUuid(null)
        dbf.update(vo)

        triggerNowLogicInDb = {
            called = true
            return Behavior.SUCCESS
        }

        gcMgr.managementNodeReady()

        retryInSecs {
            assert called
            assert dbFindByUuid(gc.uuid, GarbageCollectorVO.class).status == GCStatus.Done
        }
    }

    void testGCLoadedFromDbFailure() {
        def gc = new TimeBasedGCInDb()
        gc.NEXT_TIME = 500
        gc.NAME = "testGCLoadedFromDbFailure"
        gc.NEXT_TIME_UNIT = TimeUnit.MILLISECONDS
        gc.save()

        GarbageCollectorVO vo = dbFindByUuid(gc.uuid, GarbageCollectorVO.class)
        vo.setManagementNodeUuid(null)
        dbf.update(vo)

        triggerNowLogicInDb = {
            return Behavior.FAILURE
        }

        gcMgr.managementNodeReady()

        retryInSecs {
            vo = dbFindByUuid(gc.uuid, GarbageCollectorVO.class)
            assert vo != null
            assert vo.status == GCStatus.Idle
        }
    }

    void testGCLoadedFromDbCancel() {
        boolean called = false
        def gc = new TimeBasedGCInDb()
        gc.NAME = "testGCLoadedFromDbCancel"
        gc.NEXT_TIME = 500
        gc.NEXT_TIME_UNIT = TimeUnit.MILLISECONDS
        gc.save()

        GarbageCollectorVO vo = dbFindByUuid(gc.uuid, GarbageCollectorVO.class)
        vo.setManagementNodeUuid(null)
        dbf.update(vo)

        triggerNowLogicInDb = {
            called = true
            return Behavior.CANCEL
        }

        gcMgr.managementNodeReady()

        retryInSecs {
            assert called
            assert dbFindByUuid(gc.uuid, GarbageCollectorVO.class).status == GCStatus.Done
        }
    }

    void testGCScanOrphan() {
        boolean called = false
        def gc = new TimeBasedGCInDb()
        gc.NEXT_TIME = 500
        gc.NEXT_TIME_UNIT = TimeUnit.MILLISECONDS
        gc.NAME = "testGCScanOrphan"
        gc.save()

        GarbageCollectorVO vo = dbFindByUuid(gc.uuid, GarbageCollectorVO.class)
        vo.setManagementNodeUuid(null)
        dbf.update(vo)

        GCGlobalConfig.SCAN_ORPHAN_JOB_INTERVAL.updateValue(1)
        gcMgr.start()

        triggerNowLogicInDb = {
            called = true
            return Behavior.SUCCESS
        }

        retryInSecs {
            assert called
            assert dbFindByUuid(gc.uuid, GarbageCollectorVO.class).status == GCStatus.Done
        }
    }

    void testGCInDBTriggeredByApiWithMgmtUuidNull() {
        boolean called = false
        def gc = new TimeBasedGCInDb()
        gc.NEXT_TIME = 500
        gc.NEXT_TIME_UNIT = TimeUnit.MILLISECONDS
        gc.NAME = "testGCInDBTriggeredByApi"
        gc.save()

        GarbageCollectorVO vo = dbFindByUuid(gc.uuid, GarbageCollectorVO.class)
        vo.setManagementNodeUuid(null)
        dbf.update(vo)

        triggerNowLogicInDb = {
            called = true
            return Behavior.SUCCESS
        }

        triggerGCJob {
            uuid = gc.uuid
            sessionId = adminSessionUuid
        }

        retryInSecs {
            assert called
            assert dbFindByUuid(gc.uuid, GarbageCollectorVO.class).status == GCStatus.Done
        }
    }

    void testGCInDBTriggeredByApiWithMgmtUuidNotNull() {
        boolean called = false
        def gc = new TimeBasedGCInDb()
        gc.NEXT_TIME = 500
        gc.NEXT_TIME_UNIT = TimeUnit.MILLISECONDS
        gc.NAME = "testGCInDBTriggeredByApi"
        gc.save()

        triggerNowLogicInDb = {
            called = true
            return Behavior.SUCCESS
        }

        triggerGCJob {
            uuid = gc.uuid
            sessionId = adminSessionUuid
        }

        retryInSecs {
            assert called
            assert dbFindByUuid(gc.uuid, GarbageCollectorVO.class).status == GCStatus.Done
        }
    }

    void testQueryGCJob() {
        int count = 0

        def gc = new TimeBasedGC1()
        gc.NAME = "testQueryGCJob"
        gc.triggerNowLogic = {
            count ++
            return Behavior.SUCCESS
        }
        gc.submit(500, TimeUnit.DAYS)

        GarbageCollectorInventory inv = queryGCJob {
            conditions = ["name=${gc.NAME}".toString()]
            sessionId = adminSessionUuid
        }[0]

        assert inv.uuid == gc.uuid
        assert inv.status == GCStatus.Idle.toString()
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
        testGCInDBTriggeredByApiWithMgmtUuidNull()
        testGCInDBTriggeredByApiWithMgmtUuidNotNull()
        testQueryGCJob()
    }

    @Override
    void clean() {
        SQL.New(GarbageCollectorVO.class).delete()
    }
}
