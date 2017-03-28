package org.zstack.core.progress;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.progress.ProgressCommands.ProgressReportCmd;
import org.zstack.header.AbstractService;
import org.zstack.header.Constants;
import org.zstack.header.core.progress.*;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.*;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.rest.SyncHttpCallHandler;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.zstack.core.Platform.operr;
import static org.zstack.core.Platform.toI18nString;


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
    private CloudBus bus;

    private Map<String, Map<String, Long>> steps = new ConcurrentHashMap<>();

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
            public void beforePublishEvent(Event evt) {
                if (!(evt instanceof APIEvent)) {
                    return;
                }

                String apiName = ThreadContext.get(Constants.THREAD_CONTEXT_TASK_NAME);
                if (apiName == null || ThreadContext.get(Constants.THREAD_CONTEXT_PROGRESS_ENABLED) == null) {
                    return;
                }

                String apiId = ThreadContext.get(Constants.THREAD_CONTEXT_API);
                if (apiId == null) {
                    return;
                }

                if (steps.containsKey(apiName)) {
                    return;
                }

                new SQLBatch() {
                    @Override
                    protected void scripts() {
                        TaskStepVO vo = q(TaskStepVO.class).eq(TaskStepVO_.taskName, apiName).find();
                        if (vo == null) {
                            saveToDb();
                        } else {
                            Map<String, Long> step = JSONObjectUtil.toObject(vo.getContent(), LinkedHashMap.class);
                            steps.put(apiName, step);
                        }
                    }

                    private void saveToDb() {
                        Map<String, Long> step = new HashMap<>();

                        List<String> taskNames = q(TaskProgressVO.class).select(TaskProgressVO_.taskName)
                                .eq(TaskProgressVO_.apiId, apiId).groupBy(TaskProgressVO_.taskName).listValues();

                        for (String tn : taskNames) {
                            String puuid = sql("select vo.parentUuid from TaskProgressVO vo where vo.taskName = :tn" +
                                    " and vo.apiId = :id and type = :type group by vo.parentUuid", String.class)
                                    .param("tn", tn).param("id", apiId).param("type", TaskType.Task).limit(1).find();

                            long count = puuid == null ?
                                    sql("select count(*) from TaskProgressVO vo where vo.taskName = :tn" +
                                            " and vo.apiId = :id and type = :type and vo.parentUuid is null", Long.class)
                                            .param("tn", tn).param("id", apiId).param("type", TaskType.Task).find()
                                    :

                                    sql("select count(*) from TaskProgressVO vo where vo.taskName = :tn" +
                                            " and vo.apiId = :id and type = :type and vo.parentUuid = :puuid", Long.class)
                                            .param("tn", tn).param("id", apiId)
                                            .param("type", TaskType.Task).param("puuid", puuid).find();

                            // all entries with type = progress are counted as one entry
                            if (q(TaskProgressVO.class)
                                    .eq(TaskProgressVO_.apiId, apiId)
                                    .eq(TaskProgressVO_.taskName, tn)
                                    .eq(TaskProgressVO_.type, TaskType.Progress).isExists()) {
                                count += 1;
                            }

                            step.put(tn, count);
                        }

                        TaskStepVO vo = new TaskStepVO();
                        vo.setTaskName(apiName);
                        vo.setContent(JSONObjectUtil.toJsonString(step));
                        dbf.getEntityManager().persist(vo);

                        steps.put(apiName, step);
                    }
                }.execute();
            }
        });

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void validationType(String processType) {
        if (processType == null || !ProgressConstants.ProgressType.contains(processType) ) {
            logger.warn(String.format("not supported processType: %s", processType));
            throw new OperationFailureException(operr("not supported processType: %s",
                            processType));
        }
    }

    private void validationUuid(String uuid) {
        if (uuid == null) {
            logger.warn("not supported null uuid");
            throw new OperationFailureException(operr("not supported null uuid"));
        }
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

            int compliment = 0;
            int current = 0;
            for (TaskProgressInventory inv : e.getValue()) {
                if (inv.getType().equals(TaskType.Progress.toString())) {
                    compliment = 1;
                } else {
                    current ++;
                }
            }

            current += compliment;

            for (TaskProgressInventory inv : e.getValue()) {
                inv.setCurrentStep(current);
            }
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

            private Integer calculateCurrentStep(TaskProgressVO vo) {
                if (vo.getType() == TaskType.Progress) {
                    return null;
                }

                if (vo.getParentUuid() ==  null)  {
                    return Q.New(TaskProgressVO.class).isNull(TaskProgressVO_.parentUuid).count().intValue();
                } else {
                    return Q.New(TaskProgressVO.class).eq(TaskProgressVO_.parentUuid, vo.getParentUuid()).count().intValue();
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
                    inv.setCurrentStep(calculateCurrentStep(vo));
                    reply.setInventories(asList(inv));
                    return;
                }

                List<TaskProgressInventory> invs = new ArrayList<>();
                inv = inventory(vo);
                inv.setCurrentStep(calculateCurrentStep(vo));
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
                    inv.setCurrentStep(calculateCurrentStep(vo));
                    invs.add(inv);
                }

                Collections.reverse(invs);
                reply.setInventories(invs);
            }

            private void replyAllProgress() {
                reply.setInventories(getAllProgress(msg.getApiId()));
            }
        }.execute();
        
        setStepsNumber(reply);

        bus.reply(msg, reply);
    }

    private void setStepsNumber(APIGetTaskProgressReply reply) {
        if (reply.getInventories().isEmpty()) {
            return;
        }

        Map<String, Long> step = steps.get(reply.getInventories().get(0).getTaskName());
        if (step == null) {
            // no step count for this task, directly out
            return;
        }

        for (TaskProgressInventory inv : reply.getInventories()) {
            setStepsNumber(inv, step);
        }
    }

    private void setStepsNumber(TaskProgressInventory inv, Map<String, Long> step) {
        inv.setTotalSteps(step.get(inv.getTaskName()).intValue());

        if (inv.getSubTasks() != null) {
            for (TaskProgressInventory i : inv.getSubTasks()) {
                setStepsNumber(i, step);
            }
        }
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
        taskProgress(TaskType.Task, fmt, args);
    }

    public static void reportProgress(String fmt) {
        logger.debug(String.format("report progress is : %s", fmt));
        taskProgress(TaskType.Progress, fmt);
    }
}
