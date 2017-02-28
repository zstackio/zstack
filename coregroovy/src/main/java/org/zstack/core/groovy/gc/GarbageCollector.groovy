package org.zstack.core.groovy.gc

import org.zstack.core.Platform
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.cloudbus.EventFacade
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.errorcode.ErrorFacade
import org.zstack.core.gc.GCStatus
import org.zstack.core.gc.GarbageCollectorVO
import org.zstack.header.errorcode.ErrorCode
import org.zstack.utils.FieldUtils
import org.zstack.utils.Utils
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.logging.CLogger

import java.lang.reflect.Field
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by xing5 on 2017/3/2.
 */
abstract class GarbageCollector {
    protected DatabaseFacade dbf
    protected CloudBus bus
    protected EventFacade evtf
    protected ErrorFacade errf

    GarbageCollector() {
        dbf = bean(DatabaseFacade.class)
        bus = bean(CloudBus.class)
        evtf = bean(EventFacade.class)
        errf = bean(ErrorFacade.class)
    }

    CLogger logger = Utils.getLogger(getClass())

    // override in the subclass to give a name to the GC job
    String NAME = this.class.name
    Long id
    int executedTimes

    protected Closure canceller

    protected static <T> T bean(Class<T> clz) {
        return Platform.getComponentLoader().getComponent(clz)
    }


    AtomicBoolean lockJob = new AtomicBoolean(false)

    boolean lock() {
        return lockJob.compareAndSet(false, true)
    }

    void unlock() {
        lockJob.set(false)
    }

    protected void success() {
        assert id != null
        assert canceller != null

        logger.debug("[GC] a job[name:$NAME, id:$id] completes successfully")
        canceller()

        dbf.removeByPrimaryKey(id, GarbageCollectorVO.class)
    }

    protected void cancel() {
        assert id != null
        assert canceller != null

        logger.debug("[GC] a job[name:$NAME, id:$id] is cancelled by itself")
        canceller()

        dbf.removeByPrimaryKey(id, GarbageCollectorVO.class)
    }

    protected void fail(ErrorCode err) {
        assert id != null

        unlock()

        logger.debug("[GC] a job[name:$NAME, id:$id] failed because $err")
        GarbageCollectorVO vo = dbf.findById(id, GarbageCollectorVO.class)
        if (vo == null) {
            logger.warn("[GC] cannot find a job[name:$NAME, id:$id], assume it's deleted")
            cancel()
            return
        }

        vo.setStatus(GCStatus.Idle)
        dbf.update(vo)
    }


    final protected void saveToDb() {
        def context = [:]

        FieldUtils.getAllFields(this.class).each { Field f ->
            if (!f.isAnnotationPresent(GC.class)) {
                return
            }
            context[f.name] = getProperty(f.name)
        }

        GarbageCollectorVO vo = new GarbageCollectorVO()
        vo.setContext(JSONObjectUtil.toJsonString(context))
        vo.setRunnerClass(getClass().name)
        vo.setManagementNodeUuid(Platform.getManagementServerId())
        vo.setStatus(GCStatus.Idle)
        if (this instanceof EventBasedGarbageCollector) {
            vo.setType(GarbageCollectorType.EventBased.toString())
        } else {
            vo.setType(GarbageCollectorType.TimeBased.toString())
        }
        vo.setName(NAME)
        vo = dbf.persistAndRefresh(vo)
        id = vo.id

        logger.debug("[GC] saved a job[name:$NAME, id:$id] to DB")
    }

    protected void loadFromVO(GarbageCollectorVO vo) {
        def dataObj = JSONObjectUtil.toObject(vo.context, this.class)

        FieldUtils.getAllFields(this.class).each { Field f ->
            if (!f.isAnnotationPresent(GC.class)) {
                return
            }

            setProperty(f.name, dataObj.getProperty(f.name))
        }

        id = vo.id
        vo.status = GCStatus.Idle
        vo.managementNodeUuid = Platform.managementServerId
        dbf.update(vo)
    }
}
