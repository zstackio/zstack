package org.zstack.core.progress;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.*;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.progress.ProgressCommands.ProgressReportCmd;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.Constants;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.core.progress.*;
import org.zstack.header.longjob.APISubmitLongJobEvent;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.*;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.rest.SyncHttpCallHandler;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.ThreadContextUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import javax.persistence.Tuple;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.toI18nString;
import static org.zstack.header.Constants.THREAD_CONTEXT_API;
import static org.zstack.header.Constants.THREAD_CONTEXT_TASK_NAME;


/**
 * Created by mingjian.deng on 16/12/10.
 */
public class ProgressReportService extends AbstractService implements ManagementNodeReadyExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ProgressReportService.class);
    @Autowired
    private RESTFacade restf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private CloudBus bus;

    private int DELETE_DELAY = 300;

    private Future<Void> cleanupThread;

    private static Map<String, ParallelTaskStage> parallelTaskStage = new ConcurrentHashMap<>();

    private void startCleanupThread() {
        if (cleanupThread != null) {
            cleanupThread.cancel(true);
        }

        logger.debug(String.format("progress cleanup thread starts with interval %ss", ProgressGlobalConfig.CLEANUP_THREAD_INTERVAL.value(Integer.class)));
        cleanupThread = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.SECONDS;
            }

            @Override
            public long getInterval() {
                return ProgressGlobalConfig.CLEANUP_THREAD_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "progress-cleanup-thread";
            }

            @Override
            public void run() {
                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        Query query = dbf.getEntityManager().createNativeQuery("select unix_timestamp()");
                        Long current = ((BigInteger) query.getSingleResult()).longValue() * 1000;
                        sql(TaskProgressVO.class).notNull(TaskProgressVO_.timeToDelete)
                                .lte(TaskProgressVO_.timeToDelete, current).hardDelete();
                        sql("delete from TaskProgressVO vo where vo.time + :ttl <= UNIX_TIMESTAMP() * 1000")
                                .param("ttl", TimeUnit.SECONDS.toMillis(ProgressGlobalConfig.PROGRESS_TTL.value(Long.class))).execute();
                    }
                }.execute();
            }
        });
    }

    public void setDELETE_DELAY(int DELETE_DELAY) {
        DebugUtils.Assert(DELETE_DELAY > 0, "DELETE_DELAY must be greater than 0");
        this.DELETE_DELAY = DELETE_DELAY;
    }

    public int getDELETE_DELAY() {
        return DELETE_DELAY;
    }

    private void setThreadContext(ProgressReportCmd cmd) {
        ThreadContext.clearAll();
        if (cmd.getThreadContextMap() != null) {
            ThreadContext.putAll(cmd.getThreadContextMap());
        }
        if (cmd.getThreadContextStack() != null) {
            ThreadContext.setStack(cmd.getThreadContextStack());
        }
    }

    @Override
    public boolean start() {
        restf.registerSyncHttpCallHandler(ProgressConstants.PROGRESS_REPORT_PATH, ProgressReportCmd.class, new SyncHttpCallHandler<ProgressReportCmd>() {
            @Override
            @Deferred
            public String handleSyncHttpCall(ProgressReportCmd cmd) {
                Runnable cleanup = ThreadContextUtils.saveThreadContext();
                Defer.defer(cleanup);
                setThreadContext(cmd);
                taskProgress(TaskType.Progress, cmd.getProgress(), cmd.getDetail());
                return null;
            }
        });

        bus.installBeforePublishEventInterceptor(new AbstractBeforePublishEventInterceptor() {
            @Override
            @ExceptionSafe
            public void beforePublishEvent(Event evt) {
                if (!(evt instanceof APIEvent) || evt instanceof APISubmitLongJobEvent) {
                    return;
                }

                cleanTaskProgress(((APIEvent) evt).getApiId());
            }
        });

        ProgressGlobalConfig.CLEANUP_THREAD_INTERVAL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                startCleanupThread();
            }
        });

        startCleanupThread();

        return true;
    }

    @Transactional
    public void cleanTaskProgress(String apiId) {
        if (apiId == null) {
            return;
        }

        Query query = dbf.getEntityManager().createNativeQuery("select unix_timestamp()");
        Long current = ((BigInteger) query.getSingleResult()).longValue() * 1000;
        SQL.New(TaskProgressVO.class).eq(TaskProgressVO_.apiId, apiId).set(TaskProgressVO_.timeToDelete,
                current + TimeUnit.SECONDS.toMillis(DELETE_DELAY)).update();
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void managementNodeReady() {
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(ProgressConstants.SERVICE_ID);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIGetTaskProgressMsg) {
            handle((APIGetTaskProgressMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private TaskProgressInventory inventory(TaskProgressVO vo) {
        TaskProgressInventory inv = new TaskProgressInventory(vo);
        inv.setContent(toI18nString(vo.getContent()));

        if (!StringUtils.isEmpty(vo.getArguments())) {
            inv.setArguments(vo.getArguments());
        }
        return inv;
    }

    @Transactional
    private List<TaskProgressInventory> getAllProgress(String apiId) {
        List<TaskProgressVO> vos = Q.New(TaskProgressVO.class).eq(TaskProgressVO_.apiId, apiId).list();
        if (vos.isEmpty()) {
            return new ArrayList<>();
        }

        List<TaskProgressInventory> invs = vos.stream().map(this::inventory).collect(Collectors.toList());
        Map<String, List<TaskProgressInventory>> map = new HashMap<>();
        String nullKey = "null";
        for (TaskProgressInventory inv : invs) {
            String key = inv.getParentUuid() == null ? nullKey : inv.getParentUuid();
            List<TaskProgressInventory> lst = map.computeIfAbsent(key, k -> new ArrayList<>());
            lst.add(inv);
        }

        // sort by time with ASC
        for (Map.Entry<String, List<TaskProgressInventory>> e : map.entrySet()) {
            e.getValue().sort(Comparator.comparingLong(TaskProgressInventory::getTime));
        }

        for (Map.Entry<String, List<TaskProgressInventory>> e : map.entrySet()) {
            if (e.getKey().equals(nullKey)) {
                continue;
            }

            Optional<TaskProgressInventory> opt = invs.stream().filter(it -> e.getKey().equals(it.getTaskUuid())).findAny();
            assert opt.isPresent();
            TaskProgressInventory inv = opt.get();
            inv.setSubTasks(e.getValue());
        }

        invs = map.get(nullKey);
        return invs;
    }

    private void handle(final APIGetTaskProgressMsg msg) {
        APIGetTaskProgressReply reply = new APIGetTaskProgressReply();

        new SQLBatch() {
            @Override
            protected void scripts() {
                if (msg.isAll()) {
                    replyAllProgress();
                } else {
                    replyLastProgress();
                }
            }

            private void replyLastProgress() {
                TaskProgressVO vo = Q.New(TaskProgressVO.class)
                        .eq(TaskProgressVO_.apiId, msg.getApiId())
                        .orderBy(TaskProgressVO_.time, SimpleQuery.Od.DESC)
                        .limit(1)
                        .find();

                if (vo == null) {
                    reply.setInventories(new ArrayList<>());
                    return;
                }

                TaskProgressInventory inv;
                if (vo.getParentUuid() == null) {
                    inv = inventory(vo);
                    reply.setInventories(asList(inv));
                    return;
                }

                List<TaskProgressInventory> invs = new ArrayList<>();
                inv = inventory(vo);
                invs.add(inv);

                while (vo.getParentUuid() != null) {
                    vo = Q.New(TaskProgressVO.class)
                            .eq(TaskProgressVO_.apiId, msg.getApiId())
                            .eq(TaskProgressVO_.taskUuid, vo.getParentUuid())
                            .orderBy(TaskProgressVO_.time, SimpleQuery.Od.DESC)
                            .limit(1)
                            .find();

                    if (vo == null) {
                        break;
                    }

                    inv = inventory(vo);
                    invs.add(inv);
                }

                Collections.reverse(invs);
                reply.setInventories(invs);
            }

            private void replyAllProgress() {
                reply.setInventories(getAllProgress(msg.getApiId()));
            }
        }.execute();

        bus.reply(msg, reply);
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private static String getTaskUuid() {
        return ThreadContext.peek();
    }

    private static String getParentUuid() {
        if (ThreadContext.getImmutableStack().isEmpty()) {
            return null;
        }

        if (ThreadContext.getImmutableStack().size() == 1) {
            String uuid = ThreadContext.get(Constants.THREAD_CONTEXT_API);
            assert uuid != null;
            return uuid;
        }

        List<String> lst = ThreadContext.getImmutableStack().asList();
        return lst.get(lst.size() - 2);
    }

    public static void createSubTaskProgress(String fmt, Object... args) {
        if (!ProgressGlobalConfig.PROGRESS_ON.value(Boolean.class)) {
            return;
        }

        if (!ThreadContext.containsKey(Constants.THREAD_CONTEXT_API)) {
            if (args != null) {
                logger.warn(String.format("no task uuid found for:" + fmt, args));
            } else {
                logger.warn(String.format("no task uuid found for:" + fmt, args));
            }
            return;
        }

        ThreadContext.put(Constants.THREAD_CONTEXT_PROGRESS_ENABLED, "true");

        String parentUuid = getParentUuid();
        String taskUuid = Platform.getUuid();
        ThreadContext.push(taskUuid);
        ThreadContext.push(Platform.getUuid());

        TaskProgressVO vo = new TaskProgressVO();
        vo.setApiId(ThreadContext.get(Constants.THREAD_CONTEXT_API));
        vo.setTaskUuid(taskUuid);
        vo.setParentUuid(parentUuid);
        vo.setContent(fmt);
        if (args != null) {
            vo.setArguments(JSONObjectUtil.toJsonString(args));
        }
        vo.setType(TaskType.Task);
        vo.setTime(System.currentTimeMillis());
        vo.setManagementUuid(Platform.getManagementServerId());
        vo.setTaskName(ThreadContext.get(Constants.THREAD_CONTEXT_TASK_NAME));

        Platform.getComponentLoader().getComponent(DatabaseFacade.class).persist(vo);

        // use content as the subtask name
        ThreadContext.put(Constants.THREAD_CONTEXT_TASK_NAME, vo.getContent());
    }

    private static void calculateFmtAndPersist(TaskProgressVO vo, String apiId, TaskType type, String fmt) {
        ParallelTaskStage pstage = parallelTaskStage.get(apiId);
        if (type != TaskType.Progress || pstage == null) {
            vo.setContent(fmt);
            persistProgress(vo, type);
            return;
        }

        int percent = Integer.parseInt(fmt);
        if (pstage.isOver(percent)) {
            parallelTaskStage.remove(apiId);
            vo.setContent(fmt);
            persistProgress(vo, type);
            return;
        }

        pstage.calculatePercent(percent, p -> {
            vo.setContent(String.valueOf(p));
            vo.setTime(System.currentTimeMillis());
            persistProgress(vo, type);
        });
    }

    private static void persistProgress(TaskProgressVO vo, TaskType type) {
        if (type == TaskType.Progress) {
            logger.trace(String.format("report progress is : %s", vo.getContent()));
        }

        Platform.getComponentLoader().getComponent(DatabaseFacade.class).persist(vo);
    }

    private static void taskProgress(TaskType type, String fmt, Object... args) {
        if (!ProgressGlobalConfig.PROGRESS_ON.value(Boolean.class)) {
            return;
        }

        if (!ThreadContext.containsKey(Constants.THREAD_CONTEXT_API)) {
            if (args != null) {
                logger.warn(String.format("no task uuid found for:" + fmt, args));
            } else {
                logger.warn("no task uuid found for:" + fmt);
            }
            return;
        }

        String apiId = ThreadContext.get(THREAD_CONTEXT_API);

        if (apiId == null) {
            logger.warn("apiId not found");
            return;
        }

        ThreadContext.put(Constants.THREAD_CONTEXT_PROGRESS_ENABLED, "true");

        String taskUuid = getTaskUuid();
        if (taskUuid.isEmpty()) {
            taskUuid = Platform.getUuid();
        }

        TaskProgressVO vo = new TaskProgressVO();
        vo.setApiId(apiId);
        vo.setTaskUuid(taskUuid);
        vo.setParentUuid(getParentUuid());
        if (args != null) {
            vo.setArguments(JSONObjectUtil.toJsonString(args));
        }
        vo.setType(type);
        vo.setTime(System.currentTimeMillis());
        vo.setManagementUuid(Platform.getManagementServerId());
        vo.setTaskName(ThreadContext.get(Constants.THREAD_CONTEXT_TASK_NAME));

        calculateFmtAndPersist(vo, apiId, type, fmt);
    }

    public static void taskProgress(String fmt, Object...args) {
        if (!ProgressGlobalConfig.PROGRESS_ON.value(Boolean.class)) {
            return;
        }

        taskProgress(TaskType.Task, fmt, args);
    }

    public static void reportProgress(String fmt) {
        if (!ProgressGlobalConfig.PROGRESS_ON.value(Boolean.class)) {
            return;
        }

        taskProgress(TaskType.Progress, fmt);
    }

    public void reportProgressUntil(String end, int intervalSec) {
        reportProgressUntil(end, intervalSec, TimeUnit.SECONDS);
    }

    public void reportProgressUntil(String end, int intervalSec, TimeUnit timeUnit) {
        thdf.submitCancelablePeriodicTask(new CancelablePeriodicTask() {
            int endPercent = new Double(end).intValue();
            String apiId = ThreadContext.get(THREAD_CONTEXT_API);
            String taskName = ThreadContext.get(THREAD_CONTEXT_TASK_NAME);

            @Override
            @Deferred
            public boolean run() {
                // get current progress
                Tuple res = SQL.New("SELECT content, timeToDelete FROM TaskProgressVO" +
                        " WHERE apiId = :apiId" +
                        " AND type = :type" +
                        " ORDER BY CAST(content AS int) DESC", Tuple.class)
                        .param("apiId", apiId)
                        .param("type", TaskType.Progress)
                        .limit(1)
                        .find();

                if (res != null && res.get(1) != null) {
                    // FIXME: race condition here.
                    return true;
                }

                int currentPercent = res == null ? 0 : new Double(res.get(0, String.class)).intValue();

                Runnable cleanup = ThreadContextUtils.saveThreadContext();
                Defer.defer(cleanup);
                ThreadContext.put(THREAD_CONTEXT_API, apiId);
                ThreadContext.put(THREAD_CONTEXT_TASK_NAME, taskName);
                if (endPercent <= currentPercent) {
                    reportProgress(String.valueOf(currentPercent));
                    return true;
                } else {
                    reportProgress(String.valueOf(currentPercent + 1));
                    return false;
                }
            }

            @Override
            public TimeUnit getTimeUnit() {
                return timeUnit;
            }

            @Override
            public long getInterval() {
                return intervalSec;
            }

            @Override
            public String getName() {
                return "report-progress-one-step-at-a-time";
            }
        });
    }

    public static TaskProgressRange markTaskStage(TaskProgressRange exactStage) {
        return markTaskStage(null, exactStage);
    }


    public static TaskProgressRange markTaskStage(TaskProgressRange parentStage, TaskProgressRange subStage) {
        TaskProgressRange exactStage = parentStage != null ? transformSubStage(parentStage, subStage) : subStage;
        ThreadContext.put(Constants.THREAD_CONTEXT_TASK_STAGE, exactStage.toString());
        return exactStage;
    }

    public static ParallelTaskStage markParallelTaskStage(TaskProgressRange parentStage, TaskProgressRange subStage, List<? extends Number> weight) {
        TaskProgressRange exactStage = parentStage != null ? transformSubStage(parentStage, subStage) : subStage;
        ParallelTaskStage stage = new ParallelTaskStage(exactStage, weight);
        Optional.ofNullable(ThreadContext.get(Constants.THREAD_CONTEXT_API)).ifPresent(it -> parallelTaskStage.put(it, stage));
        return stage;
    }

    public static ParallelTaskStage markParallelTaskExactStage(TaskProgressRange exactStage, List<? extends Number> weight) {
        ParallelTaskStage stage = new ParallelTaskStage(exactStage, weight);
        Optional.ofNullable(ThreadContext.get(Constants.THREAD_CONTEXT_API)).ifPresent(it -> parallelTaskStage.put(it, stage));
        return stage;
    }

    public static TaskProgressRange getTaskStage() {
        String stage = ThreadContext.get(Constants.THREAD_CONTEXT_TASK_STAGE) != null ?
                ThreadContext.get(Constants.THREAD_CONTEXT_TASK_STAGE) : "0-100";
        return TaskProgressRange.valueOf(stage);
    }

    public static List<TaskProgressRange> splitTaskStage(TaskProgressRange stage, List<? extends Number> weight) {
        List<TaskProgressRange> results = new ArrayList<>();
        double total = weight.stream().mapToDouble(Number::doubleValue).sum();
        if (total == 0) {
            return splitTaskStage(stage, weight.stream().map(it -> 1).collect(Collectors.toList()));
        }

        int range = stage.getEnd() - stage.getStart();
        double end = stage.getStart();
        for (Number w : weight) {
            results.add(new TaskProgressRange((int) end, (int) (end += (w.doubleValue() / total * range))));
        }
        return results;
    }


    private static TaskProgressRange transformSubStage(TaskProgressRange parentStage, TaskProgressRange subStage) {
        float ratio = (float) (parentStage.getEnd() - parentStage.getStart()) / 100;
        int exactStart = Math.round(subStage.getStart() * ratio + parentStage.getStart());
        int exactEnd = Math.round(subStage.getEnd() * ratio + parentStage.getStart());
        return new TaskProgressRange(exactStart, exactEnd);
    }
}
