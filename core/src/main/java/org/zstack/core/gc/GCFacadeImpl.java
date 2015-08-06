package org.zstack.core.gc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 8/5/2015.
 */
public class GCFacadeImpl implements GCFacade, ManagementNodeChangeListener {
    private static final CLogger logger = Utils.getLogger(GCFacadeImpl.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ResourceDestinationMaker destinationMaker;

    private GarbageCollectorVO save(GCContext context) {
        DebugUtils.Assert(context.getTimeUnit() != null, "timeUnit cannot be null");
        DebugUtils.Assert(context.getInterval() > 0, "interval must be greater than 0");
        DebugUtils.Assert(context.getRunnerClass() != null, "runnerClass cannot be null");
        DebugUtils.Assert(GCRunner.class.isAssignableFrom(context.getRunnerClass()), "runnerClass must be a implementation of GCRunner");

        GarbageCollectorVO vo = new GarbageCollectorVO();
        vo.setContext(context.toInternal().toJson());
        vo.setRunnerClass(context.getRunnerClass().getName());
        vo.setManagementNodeUuid(Platform.getManagementServerId());
        vo.setStatus(GCStatus.Idle);
        vo = dbf.persistAndRefresh(vo);
        return vo;
    }

    private GCRunner getGCRunner(GCContext context) {
        try {
            return (GCRunner) context.getRunnerClass().newInstance();
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    private void scheduleTask(final GCContext context, final GarbageCollectorVO vo, boolean instant) {
        final GCCompletion completion = new GCCompletion() {
            @Override
            public void success() {
                vo.setStatus(GCStatus.Done);
                dbf.update(vo);
                logger.debug(String.format("GC job[id:%s, runner class:%s] is done", vo.getId(), vo.getRunnerClass()));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.debug(String.format("GC job[id:%s, runner class:%s] failed, %s. Reschedule it", vo.getId(), vo.getRunnerClass(), errorCode));
                scheduleTask(context, vo, true);
            }

            @Override
            public void cancel() {
                vo.setStatus(GCStatus.Idle);
                dbf.update(vo);
                logger.debug(String.format("GC job[id:%s, runner class:%s] is cancelled by the runner, set it to idle", vo.getId(), vo.getRunnerClass()));
            }
        };

        final GCRunner runner = getGCRunner(context);
        CancelablePeriodicTask ct = new CancelablePeriodicTask() {
            @Override
            public boolean run() {
                vo.setStatus(GCStatus.Processing);
                dbf.update(vo);
                logger.debug(String.format("starting GC job[id:%s, runner class:%s]", vo.getId(), vo.getRunnerClass()));
                runner.run(context, completion);
                return true;
            }

            @Override
            public TimeUnit getTimeUnit() {
                return context.getTimeUnit();
            }

            @Override
            public long getInterval() {
                return context.getInterval();
            }

            @Override
            public String getName() {
                return String.format("gc-%s", vo.getId());
            }
        };

        if (instant) {
            thdf.submitCancelablePeriodicTask(ct);
        } else {
            thdf.submitCancelablePeriodicTask(ct, context.getTimeUnit().toMillis(context.getInterval()));
        }
    }

    @Override
    public void schedule(final GCContext context) {
        scheduleTask(context, save(context), false);
    }

    @Override
    public void scheduleImmediately(GCContext context) {
        scheduleTask(context, save(context), true);
    }

    @Override
    public void nodeJoin(String nodeId) {
    }

    @Override
    public void nodeLeft(String nodeId) {
        setJobsToIdle(nodeId);
    }

    @Transactional
    private void setJobsToIdle(String mgmtUuid) {
        String sql = "update GarbageCollectorVO vo set vo.managementNodeUuid = null, vo.status = :status where vo.managementNodeUuid = :uuid";
        Query q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("uuid", mgmtUuid);
        q.setParameter("status", GCStatus.Idle);
        q.executeUpdate();
    }

    @Override
    public void iAmDead(String nodeId) {
        setJobsToIdle(nodeId);
    }

    @Override
    @AsyncThread
    public void iJoin(String nodeId) {
        SimpleQuery<GarbageCollectorVO> q = dbf.createQuery(GarbageCollectorVO.class);
        q.select(GarbageCollectorVO_.id);
        q.add(GarbageCollectorVO_.status, Op.IN, list(GCStatus.Idle, GCStatus.Processing));
        q.add(GarbageCollectorVO_.managementNodeUuid, Op.NULL);
        List<Long> ids = q.listValue();

        List<Long> ours = new ArrayList<Long>();
        for (long id : ids) {
            if (destinationMaker.isManagedByUs(String.valueOf(id))) {
                ours.add(id);
            }
        }

        if (ours.isEmpty()) {
            return;
        }

        q = dbf.createQuery(GarbageCollectorVO.class);
        q.add(GarbageCollectorVO_.id, Op.IN, ours);
        List<GarbageCollectorVO> vos = q.list();
        for (GarbageCollectorVO vo : vos) {
            scheduleTask(new GCContextInternal(vo).toGCContext(), vo, true);
        }
    }
}
