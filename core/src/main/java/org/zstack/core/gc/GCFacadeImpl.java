package org.zstack.core.gc;

import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.TimeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.persistence.Query;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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
    @Autowired
    private EventFacade evtf;

    private File scriptFolder;
    private GroovyScriptEngine gse;

    private long scanJobIntervalInMillis;

    void init() {
        String scriptFolderPath = PathUtil.join(CoreGlobalProperty.USER_HOME, "garbage_collector_script");
        scriptFolder = new File(scriptFolderPath);
        if (!scriptFolder.exists()) {
            scriptFolder.mkdirs();
        }

        try {
            gse = new GroovyScriptEngine(new URL[]{scriptFolder.toURI().toURL()});
        } catch (MalformedURLException e) {
            throw new CloudRuntimeException(e);
        }

        scanJobIntervalInMillis = TimeUtils.parseTimeInMillis(GCGlobalProperty.SCAN_JOB_INTERVAL);
    }

    private GarbageCollectorVO save(TimeBasedGCPersistentContext context) {
        DebugUtils.Assert(context.getTimeUnit() != null, "timeUnit cannot be null");
        DebugUtils.Assert(context.getInterval() > 0, "interval must be greater than 0");
        DebugUtils.Assert(context.getRunnerClass() != null, "runnerClass cannot be null");
        DebugUtils.Assert(GCRunner.class.isAssignableFrom(context.getRunnerClass()), "runnerClass must be a implementation of GCRunner");

        GarbageCollectorVO vo = new GarbageCollectorVO();
        vo.setContext(context.toInternal().toJson());
        vo.setRunnerClass(context.getRunnerClass().getName());
        vo.setManagementNodeUuid(Platform.getManagementServerId());
        vo.setStatus(GCStatus.Idle);
        vo.setType(TimeBasedGCPersistentContext.class.getName());
        vo = dbf.persistAndRefresh(vo);
        return vo;
    }

    private GarbageCollectorVO save(EventBasedGCPersistentContext context) {
        DebugUtils.Assert(context.getRunnerClass() != null, "runnerClass cannot be null");
        DebugUtils.Assert(GCRunner.class.isAssignableFrom(context.getRunnerClass()), "runnerClass must be a implementation of GCRunner");
        DebugUtils.Assert(context.getTriggers() != null, "triggers cannot be null");
        DebugUtils.Assert(!context.getTriggers().isEmpty(), "triggers cannot be empty");

        List<GCEventTrigger> triggers = context.getTriggers();
        for (GCEventTrigger t : triggers) {
            DebugUtils.Assert(t.getEventPath() != null, "eventPath cannot be null");
            DebugUtils.Assert(t.getCode() != null, "code cannot be null");
            DebugUtils.Assert(t.getCodeName() != null, "codeName cannot be null");
        }

        GarbageCollectorVO vo = new GarbageCollectorVO();
        vo.setContext(context.toInternal().toJson());
        vo.setRunnerClass(context.getRunnerClass().getName());
        vo.setManagementNodeUuid(Platform.getManagementServerId());
        vo.setStatus(GCStatus.Idle);
        vo.setType(EventBasedGCPersistentContext.class.getName());
        vo = dbf.persistAndRefresh(vo);
        return vo;
    }

    private GCRunner getGCRunner(GCContext context) {
        try {
            if (context instanceof TimeBasedGCPersistentContext) {
                return (GCRunner) ((TimeBasedGCPersistentContext) context).getRunnerClass().newInstance();
            } else if (context instanceof EventBasedGCPersistentContext) {
                return (GCRunner) ((EventBasedGCPersistentContext) context).getRunnerClass().newInstance();
            } else if (context instanceof TimeBasedGCEphemeralContext) {
                return ((TimeBasedGCEphemeralContext) context).getRunner();
            } else if (context instanceof EventBasedGCEphemeralContext) {
                return ((EventBasedGCEphemeralContext) context).getRunner();
            } else {
                throw new CloudRuntimeException("should not be here");
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    class RunningOnce {
        final AtomicBoolean isRunning = new AtomicBoolean(false);

        boolean setToRun() {
            return isRunning.compareAndSet(false, true);
        }

        void setNotRun() {
            isRunning.set(false);
        }
    }

    interface CancelEventCallback {
        void cancel();
    }

    private CancelEventCallback setupEventTrigger(final AbstractEventBasedGCContext context, final Runnable runner) {
        List<GCEventTrigger> triggers = context.getTriggers();

        List<String> ids = triggers.stream().map(GCEventTrigger::getEventPath).collect(Collectors.toList());
        logger.debug(String.format("[GC] setup the trigger on the canonical events %s", ids));

        final List<EventCallback> cbs = new ArrayList<EventCallback>();

        for (final GCEventTrigger trigger : triggers) {
            String scriptName = String.format("%s.groovy", trigger.getCodeName());
            scriptName = scriptName.replaceAll(" ", "_");
            String scriptPath = PathUtil.join(scriptFolder.getAbsolutePath(), scriptName);
            try {
                FileUtils.writeStringToFile(new File(scriptPath), trigger.getCode());
            } catch (IOException e) {
                throw new CloudRuntimeException(e);
            }

            final String finalScriptName = scriptName;
            final EventCallback cb = new EventCallback() {
                @Override
                protected void run(Map tokens, Object data) {
                    if (!Platform.getManagementServerId().equals(tokens.get(EventFacade.META_DATA_MANAGEMENT_NODE_ID))) {
                        return;
                    }

                    try {
                        Binding binding = new Binding();
                        binding.setVariable("tokens", tokens);
                        binding.setVariable("data", data);
                        binding.setVariable("context", context.getContext());
                        boolean ret = (Boolean) gse.run(finalScriptName, binding);
                        if (ret) {
                            logger.debug(String.format("[GC] code[%s], event[%s] triggered a GC job[%s]",
                                    trigger.getCodeName(), trigger.getEventPath(), context.getName()));
                            runner.run();
                        }
                    } catch (Exception e) {
                        throw new CloudRuntimeException(e);
                    }
                }
            };

            evtf.on(trigger.getEventPath(), cb);
            cbs.add(cb);
        }

        return () -> {
            for (EventCallback cb : cbs) {
                logger.debug(String.format("[GC] unlisten event for Job[%s] as it's done or cancelled", context.getName()));
                evtf.off(cb);
            }
        };
    }

    class EventCanceller {
        CancelEventCallback canceller;
    }

    private void scheduleTask(final EventBasedGCPersistentContext context, final GarbageCollectorVO vo, final boolean updateDb) {
        final EventCanceller canceller = new EventCanceller();

        final RunningOnce once = new RunningOnce();

        final GCCompletion completion = new GCCompletion() {
            @Override
            public void success() {
                vo.setStatus(GCStatus.Done);
                dbf.update(vo);

                if (canceller.canceller != null) {
                    canceller.canceller.cancel();
                }

                logger.debug(String.format("GC job[id:%s, name: %s, runner class:%s] is done",
                        vo.getId(), context.getName(), vo.getRunnerClass()));

                once.setNotRun();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.debug(String.format("GC job[id:%s, name:%s, runner class:%s] failed, %s. Reschedule it",
                        vo.getId(), context.getName(), vo.getRunnerClass(), errorCode));
                // already scheduled, no need to schedule again

                once.setNotRun();
            }

            @Override
            public void cancel() {
                vo.setStatus(GCStatus.Idle);
                dbf.update(vo);
                logger.debug(String.format("GC job[id:%s, name: %s, runner class:%s] is cancelled by the runner, set it to idle",
                        vo.getId(), context.getName(), vo.getRunnerClass()));

                once.setNotRun();
            }
        };

        final GCRunner runner = getGCRunner(context);
        final Runnable r = () -> {
            if (!once.setToRun()) {
                return;
            }

            context.increaseExecutedTime();

            if (updateDb) {
                vo.setStatus(GCStatus.Processing);
                dbf.update(vo);
            }

            logger.debug(String.format("start running GC job[id:%s, name: %s, runner class:%s], already executed %s times",
                    vo.getId(), context.getName(), vo.getRunnerClass(), context.getExecutedTimes()));
            runner.run(context, completion);
        };

        canceller.canceller = setupEventTrigger(context, r);
    }

    private void scheduleTask(final TimeBasedGCPersistentContext context, final GarbageCollectorVO vo, boolean instant, final boolean updateDb) {
        final RunningOnce once = new RunningOnce();

        final GCCompletion completion = new GCCompletion() {
            @Override
            public void success() {
                vo.setStatus(GCStatus.Done);
                dbf.update(vo);
                logger.debug(String.format("GC job[id:%s, name: %s, runner class:%s] is done",
                        vo.getId(), context.getName(), vo.getRunnerClass()));

                once.setNotRun();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.debug(String.format("GC job[id:%s, name:%s, runner class:%s] failed, %s. Reschedule it",
                        vo.getId(), context.getName(), vo.getRunnerClass(), errorCode));
                scheduleTask(context, vo, false, false);

                once.setNotRun();
            }

            @Override
            public void cancel() {
                vo.setStatus(GCStatus.Idle);
                dbf.update(vo);
                logger.debug(String.format("GC job[id:%s, name: %s, runner class:%s] is cancelled by the runner, set it to idle",
                        vo.getId(), context.getName(), vo.getRunnerClass()));

                once.setNotRun();
            }
        };

        final GCRunner runner = getGCRunner(context);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (!once.setToRun()) {
                    return;
                }

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
        DebugUtils.Assert(context.getName() != null, "context.getName() cannot be null");

        if (context instanceof TimeBasedGCPersistentContext) {
            scheduleTask((TimeBasedGCPersistentContext) context, save((TimeBasedGCPersistentContext) context), false, true);
        } else if (context instanceof EventBasedGCPersistentContext) {
            scheduleTask((EventBasedGCPersistentContext) context, save((EventBasedGCPersistentContext) context), true);
        } else if (context instanceof TimeBasedGCEphemeralContext) {
            scheduleTask((TimeBasedGCEphemeralContext) context, false);
        } else if (context instanceof EventBasedGCEphemeralContext) {
            scheduleTask((EventBasedGCEphemeralContext) context);
        }
    }

    private void scheduleTask(final EventBasedGCEphemeralContext context) {
        DebugUtils.Assert(context.getTriggers() != null, "triggers cannot be null");
        DebugUtils.Assert(!context.getTriggers().isEmpty(), "triggers cannot be empty");

        List<GCEventTrigger> triggers = context.getTriggers();
        for (GCEventTrigger t : triggers) {
            DebugUtils.Assert(t.getEventPath() != null, "eventPath cannot be null");
            DebugUtils.Assert(t.getCode() != null, "code cannot be null");
            DebugUtils.Assert(t.getCodeName() != null, "codeName cannot be null");
        }

        final EventCanceller canceller = new EventCanceller();

        final RunningOnce once = new RunningOnce();

        final GCCompletion completion = new GCCompletion() {
            @Override
            public void success() {
                logger.debug(String.format("GC ephemeral job[name:%s] is done", context.getName()));

                if (canceller.canceller != null) {
                    canceller.canceller.cancel();
                }

                once.setNotRun();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("GC ephemeral job[name:%s] failed, %s. Reschedule it", context.getName(), errorCode));
                // the job is scheduled, no need to schedule again

                once.setNotRun();
            }

            @Override
            public void cancel() {
                logger.debug(String.format("GC ephemeral job[name:%s] is cancelled by the runner", context.getName()));

                if (canceller.canceller != null) {
                    canceller.canceller.cancel();
                }

                once.setNotRun();
            }
        };

        final GCRunner runner = getGCRunner(context);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (!once.setToRun()) {
                    // is already running
                    return;
                }

                context.increaseExecutedTime();
                logger.debug(String.format("start running GC ephemeral job[name:%s], already executed %s times",
                        context.getName(), context.getExecutedTimes()));
                runner.run(context, completion);
            }
        };

        canceller.canceller = setupEventTrigger(context, r);
    }

    private void scheduleTask(final TimeBasedGCEphemeralContext context, boolean instant) {
        final RunningOnce once = new RunningOnce();

        final GCCompletion completion = new GCCompletion() {
            @Override
            public void success() {
                logger.debug(String.format("GC ephemeral job[name:%s] is done", context.getName()));
                once.setNotRun();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.debug(String.format("GC ephemeral job[name:%s] failed, %s. Reschedule it", context.getName(), errorCode));
                scheduleTask(context, false);
                once.setNotRun();
            }

            @Override
            public void cancel() {
                logger.debug(String.format("GC ephemeral job[name:%s] is cancelled by the runner", context.getName()));
                once.setNotRun();
            }
        };

        final GCRunner runner = getGCRunner(context);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (!once.setToRun()) {
                    return;
                }

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
        DebugUtils.Assert(context.getName() != null, "context.getName() cannot be null");

        if (context instanceof TimeBasedGCPersistentContext) {
            scheduleTask((TimeBasedGCPersistentContext) context, save((TimeBasedGCPersistentContext) context), true, true);
        } else {
            scheduleTask((TimeBasedGCEphemeralContext) context, true);
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
        String sql = "update GarbageCollectorVO vo" +
                " set vo.managementNodeUuid = null, vo.status = :status" +
                " where vo.managementNodeUuid = :uuid" +
                " and vo.status != :done";
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

    private void loadJobs() {
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
            logger.debug("no GC jobs managed by us");
            return;
        }

        logger.debug(String.format("GC is going to load %s jobs", ours.size()));

        q = dbf.createQuery(GarbageCollectorVO.class);
        q.add(GarbageCollectorVO_.id, Op.IN, ours);
        List<GarbageCollectorVO> vos = q.list();
        for (GarbageCollectorVO vo : vos) {
            if (TimeBasedGCPersistentContext.class.getName().equals(vo.getType())) {
                scheduleTask(new TimeBasedGCPersistentContextInternal(vo).toGCContext(), vo, true, true);
            } else if (EventBasedGCPersistentContext.class.getName().equals(vo.getType())) {
                scheduleTask(new EventBasedGCPersistentContextInternal(vo).toGCContext(), vo, true);
            } else {
                logger.warn(String.format("cannot load the GC job[id:%s], unknown type[%s]", vo.getId(), vo.getType()));
            }
        }
    }

    @AsyncThread
    @Override
    public void managementNodeReady() {
        logger.debug(String.format("Management node[uuid:%s] joins, start loading GC jobs...",
                Platform.getManagementServerId()));
        loadJobs();

        thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.MILLISECONDS;
            }

            @Override
            public long getInterval() {
                return scanJobIntervalInMillis;
            }

            @Override
            public String getName() {
                return "scan-GC-job";
            }

            @Override
            public void run() {
                loadJobs();
            }
        }, scanJobIntervalInMillis + new Random().nextInt((int) TimeUnit.SECONDS.toMillis(30)));
    }
}
