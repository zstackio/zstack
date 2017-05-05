package org.zstack.core.progress;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.*;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.progress.ProgressCommands.ProgressReportCmd;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.Constants;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.core.progress.*;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.*;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.rest.SyncHttpCallHandler;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;

import static org.zstack.core.Platform.toI18nString;

import javax.persistence.Query;
import javax.persistence.TypedQuery;


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
            public String handleSyncHttpCall(ProgressReportCmd cmd) {
                setThreadContext(cmd);
                logger.debug(String.format("report progress is : %s", cmd.getProgress()));
                taskProgress(TaskType.Progress, cmd.getProgress());
                return null;
            }
        });

        bus.installBeforePublishEventInterceptor(new AbstractBeforePublishEventInterceptor() {
            @Override
            @ExceptionSafe
            public void beforePublishEvent(Event evt) {
                if (!(evt instanceof APIEvent)) {
                    return;
                }

                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        Query query = dbf.getEntityManager().createNativeQuery("select unix_timestamp()");
                        Long current = ((BigInteger) query.getSingleResult()).longValue() * 1000;
                        sql(TaskProgressVO.class).eq(TaskProgressVO_.apiId, ((APIEvent) evt).getApiId()).set(TaskProgressVO_.timeToDelete,
                                current + TimeUnit.SECONDS.toMillis(DELETE_DELAY)).update();
                    }
                }.execute();
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
        if (vo.getArguments() == null) {
            inv.setContent(toI18nString(vo.getContent()));
        } else {
            List<String> args = JSONObjectUtil.toCollection(vo.getArguments(), ArrayList.class, String.class);
            inv.setContent(toI18nString(vo.getContent(), args.toArray()));
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
            e.getValue().sort((o1, o2) -> (int) (o1.getTime() - o2.getTime()));
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
        return lst.get(lst.size()-2);
    }

    public static void createSubTaskProgress(String fmt, Object...args) {
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

    private static void taskProgress(TaskType type, String fmt, Object...args) {
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

        ThreadContext.put(Constants.THREAD_CONTEXT_PROGRESS_ENABLED, "true");

        String taskUuid = getTaskUuid();
        if (taskUuid.isEmpty()) {
            taskUuid = Platform.getUuid();
        }

        TaskProgressVO vo = new TaskProgressVO();
        vo.setApiId(ThreadContext.get(Constants.THREAD_CONTEXT_API));
        vo.setTaskUuid(taskUuid);
        vo.setParentUuid(getParentUuid());
        vo.setContent(fmt);
        if (args != null) {
            vo.setArguments(JSONObjectUtil.toJsonString(args));
        }
        vo.setType(type);
        vo.setTime(System.currentTimeMillis());
        vo.setManagementUuid(Platform.getManagementServerId());
        vo.setTaskName(ThreadContext.get(Constants.THREAD_CONTEXT_TASK_NAME));

        Platform.getComponentLoader().getComponent(DatabaseFacade.class).persist(vo);
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

        logger.debug(String.format("report progress is : %s", fmt));
        taskProgress(TaskType.Progress, fmt);
    }
}
