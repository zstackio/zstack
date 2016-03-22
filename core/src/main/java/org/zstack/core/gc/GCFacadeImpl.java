package org.zstack.core.gc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 8/5/2015.
 */
public class GCFacadeImpl implements GCFacade, ManagementNodeChangeListener, ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(GCFacadeImpl.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ResourceDestinationMaker destinationMaker;

    private GarbageCollectorVO save(GCPersistentContext context) {
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
        if (context instanceof GCPersistentContext) {
            try {
                return (GCRunner)((GCPersistentContext)context).getRunnerClass().newInstance();
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            }
        } else {
            return ((GCEphemeralContext)context).getRunner();
        }
    }

    private void scheduleTask(final GCPersistentContext context, final GarbageCollectorVO vo, boolean instant, final boolean updateDb) {
        final GCCompletion completion = new GCCompletion() {
            @Override
            public void success() {
                vo.setStatus(GCStatus.Done);
                dbf.update(vo);
                logger.debug(String.format("GC job[id:%s, name: %s, runner class:%s] is done", vo.getId(), context.getName(), vo.getRunnerClass()));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.debug(String.format("GC job[id:%s, name:%s, runner class:%s] failed, %s. Reschedule it", vo.getId(), context.getName(), vo.getRunnerClass(), errorCode));
                scheduleTask(context, vo, false, false);
            }

            @Override
            public void cancel() {
                vo.setStatus(GCStatus.Idle);
                dbf.update(vo);
                logger.debug(String.format("GC job[id:%s, name: %s, runner class:%s] is cancelled by the runner, set it to idle", vo.getId(), context.getName(), vo.getRunnerClass()));
            }
        };

        final GCRunner runner = getGCRunner(context);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                context.increaseExecutedTime();

                if (updateDb) {
                    vo.setStatus(GCStatus.Processing);
                    dbf.update(vo);
                }

                logger.debug(String.format("start running GC job[id:%s, name: %s, runner class:%s], already executed %s times",
                        vo.getId(), context.getName(), vo.getRunnerClass(), context.getExecutedTimes()));
                runner.run(context, completion);
            }
        };

        if (instant) {
            thdf.submitTimeoutTask(r, context.getTimeUnit(), 0);
        } else {
            thdf.submitTimeoutTask(r, context.getTimeUnit(), context.getInterval());
        }
    }

    @Override
    public void schedule(final GCContext context) {
        if (context instanceof GCPersistentContext) {
            scheduleTask((GCPersistentContext) context, save((GCPersistentContext) context), false, true);
        } else {
            scheduleTask((GCEphemeralContext) context, false);
        }
    }

    private void scheduleTask(final GCEphemeralContext context, boolean instant) {
        final GCCompletion completion = new GCCompletion() {
            @Override
            public void success() {
                logger.debug(String.format("GC ephemeral job[name:%s] is done", context.getName()));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.debug(String.format("GC ephemeral job[name:%s] failed, %s. Reschedule it", context.getName(), errorCode));
                scheduleTask(context, false);
            }

            @Override
            public void cancel() {
                logger.debug(String.format("GC ephemeral job[name:%s] is cancelled by the runner", context.getName()));
            }
        };

        final GCRunner runner = getGCRunner(context);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                context.increaseExecutedTime();
                logger.debug(String.format("start running GC ephemeral job[name:%s], already executed %s times",
                        context.getName(), context.getExecutedTimes()));
                runner.run(context, completion);
            }
        };

        if (instant) {
            thdf.submitTimeoutTask(r, context.getTimeUnit(), 0);
        } else {
            thdf.submitTimeoutTask(r, context.getTimeUnit(), context.getInterval());
        }
    }

    @Override
    public void scheduleImmediately(GCContext context) {
        if (context instanceof GCPersistentContext) {
            scheduleTask((GCPersistentContext) context, save((GCPersistentContext) context), true, true);
        } else {
            scheduleTask((GCEphemeralContext)context, true);
        }
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
        String sql = "update GarbageCollectorVO vo set vo.managementNodeUuid = null, vo.status = :status where vo.managementNodeUuid = :uuid and vo.status != :done";
        Query q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("uuid", mgmtUuid);
        q.setParameter("status", GCStatus.Idle);
        q.setParameter("done", GCStatus.Done);
        q.executeUpdate();
    }

    @Override
    public void iAmDead(String nodeId) {
        setJobsToIdle(nodeId);
    }

    @Override
    public void iJoin(String nodeId) {
    }

    @Override
    @AsyncThread
    public void managementNodeReady() {
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
            scheduleTask(new GCPersistentContextInternal(vo).toGCContext(), vo, true, true);
        }
    }
}
