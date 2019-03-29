package org.zstack.longjob;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.config.GlobalConfigValidatorExtensionPoint;
import org.zstack.core.db.*;
import org.zstack.core.progress.ProgressReportService;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.timeout.ApiTimeoutExtensionPoint;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.AbstractService;
import org.zstack.header.Constants;
import org.zstack.header.core.AsyncBackup;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.APIDeleteAccountEvent;
import org.zstack.header.longjob.*;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.tag.TagManager;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.sql.SQLNonTransientConnectionException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static org.zstack.core.db.DBSourceUtils.isDBConnected;
import static org.zstack.core.db.DBSourceUtils.waitDBConnected;
import static org.zstack.core.progress.ProgressReportService.reportProgress;
import static org.zstack.header.longjob.LongJobConstants.LongJobOperation;
import static org.zstack.longjob.LongJobUtils.jobCompleted;
import static org.zstack.longjob.LongJobUtils.updateByUuid;

/**
 * Created by GuoYi on 11/14/17.
 */
public class LongJobManagerImpl extends AbstractService implements LongJobManager, ManagementNodeReadyExtensionPoint
        , ManagementNodeChangeListener, ApiTimeoutExtensionPoint {
    private static final CLogger logger = Utils.getLogger(LongJobManagerImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private ProgressReportService progRpt;
    @Autowired
    protected ApiTimeoutManager timeoutMgr;
    @Autowired
    private PluginRegistry pluginRgty;
    private List<LongJobExtensionPoint> exts = new ArrayList<>();
    @Autowired
    private transient ResourceDestinationMaker destinationMaker;

    // we need a longjob factory to produce LongJob based on JobName
    @Autowired
    private LongJobFactory longJobFactory;

    private List<String> longJobClasses = new ArrayList<String>();
    private Map<String, Class<? extends APIMessage>> useApiTimeout = new HashMap<>();
    private Map<String, Function<APIEvent, Void>> longJobCallBacks = new ConcurrentHashMap<>();

    private void collectLongJobs() {
        Set<Class<?>> subs = BeanUtils.reflections.getTypesAnnotatedWith(LongJobFor.class);
        for (Class sub : subs) {
            UseApiTimeout timeout = (UseApiTimeout) sub.getAnnotation(UseApiTimeout.class);
            if (timeout != null) {
                useApiTimeout.put(sub.toString(), timeout.value());
            }

            longJobClasses.add(sub.toString());
        }
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APISubmitLongJobMsg) {
            handle((APISubmitLongJobMsg) msg);
        } else if (msg instanceof APICancelLongJobMsg) {
            handle((APICancelLongJobMsg) msg);
        } else if (msg instanceof APIDeleteLongJobMsg) {
            handle((APIDeleteLongJobMsg) msg);
        } else if (msg instanceof APIRerunLongJobMsg) {
            handle((APIRerunLongJobMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof SubmitLongJobMsg) {
            handle((SubmitLongJobMsg) msg);
        } else if (msg instanceof CancelLongJobMsg) {
            handle((CancelLongJobMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIRerunLongJobMsg msg) {
        APIRerunLongJobEvent evt = new APIRerunLongJobEvent(msg.getId());
        SubmitLongJobMsg smsg = new SubmitLongJobMsg();
        LongJobVO job = dbf.findByUuid(msg.getUuid(), LongJobVO.class);
        smsg.setJobUuid(job.getUuid());
        smsg.setDescription(job.getDescription());
        smsg.setJobData(job.getJobData());
        smsg.setJobName(job.getJobName());
        smsg.setName(job.getName());
        smsg.setTargetResourceUuid(job.getTargetResourceUuid());
        smsg.setResourceUuid(job.getUuid());
        smsg.setSystemTags(msg.getSystemTags());
        smsg.setUserTags(msg.getUserTags());
        smsg.setAccountUuid(msg.getSession().getAccountUuid());
        bus.makeLocalServiceId(smsg, LongJobConstants.SERVICE_ID);
        bus.send(smsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply rly) {
                SubmitLongJobReply reply = rly.castReply();
                evt.setInventory(reply.getInventory());
                bus.publish(evt);
            }
        });
    }

    private void handle(APIDeleteLongJobMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "longjob-" + msg.getUuid();
            }

            @Override
            public void run(SyncTaskChain chain) {
                final APIDeleteAccountEvent evt = new APIDeleteAccountEvent(msg.getId());
                LongJobVO vo = dbf.findByUuid(msg.getUuid(), LongJobVO.class);
                dbf.remove(vo);
                logger.info(String.format("longjob [uuid:%s, name:%s] has been deleted", vo.getUuid(), vo.getName()));
                bus.publish(evt);

                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(APICancelLongJobMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "longjob-" + msg.getUuid();
            }

            @Override
            public void run(SyncTaskChain chain) {
                final APICancelLongJobEvent evt = new APICancelLongJobEvent(msg.getId());
                cancelLongJob(msg.getUuid(), new Completion(chain) {
                    @Override
                    public void success() {
                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(CancelLongJobMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "longjob-" + msg.getUuid();
            }

            @Override
            public void run(SyncTaskChain chain) {
                final CancelLongJobReply reply = new CancelLongJobReply();
                cancelLongJob(msg.getUuid(), new Completion(chain) {
                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void cancelLongJob(String uuid, Completion completion) {
        LongJobVO vo = updateByUuid(uuid, it -> it.setState(LongJobState.Canceling));
        LongJob job = longJobFactory.getLongJob(vo.getJobName());
        logger.info(String.format("longjob [uuid:%s, name:%s] has been marked canceling", vo.getUuid(), vo.getName()));

        job.cancel(vo, new Completion(completion) {
            @Override
            public void success() {
                updateByUuid(uuid, it -> it.setState(LongJobState.Canceled));
                logger.info(String.format("longjob [uuid:%s, name:%s] has been canceled", vo.getUuid(), vo.getName()));
                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                updateByUuid(uuid, it -> it.setState(LongJobState.Failed));
                logger.error(String.format("failed to cancel longjob [uuid:%s, name:%s]", vo.getUuid(), vo.getName()));
                completion.fail(errorCode);
            }
        });
    }

    private void handle(APISubmitLongJobMsg msg) {
        APISubmitLongJobEvent evt = new APISubmitLongJobEvent(msg.getId());
        SubmitLongJobMsg smsg = SubmitLongJobMsg.valueOf(msg);
        bus.makeLocalServiceId(smsg, LongJobConstants.SERVICE_ID);
        bus.send(smsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply rly) {
                SubmitLongJobReply reply = rly.castReply();
                evt.setInventory(reply.getInventory());
                evt.setNeedAudit(reply.isNeedAudit());
                bus.publish(evt);
            }
        });
    }

    private void handle(SubmitLongJobMsg msg) {
        // create new LongJobVO or get old LongJobVO
        LongJobVO vo = null;
        if (msg.getResourceUuid() != null) {
            vo = dbf.findByUuid(msg.getResourceUuid(), LongJobVO.class);
            vo.setApiId(ThreadContext.getImmutableContext().get(Constants.THREAD_CONTEXT_API));
            vo.setState(LongJobState.Waiting);
            vo.setExecuteTime(null);
            vo.setJobResult(null);
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            vo.setCreateDate(now);
            vo.setLastOpDate(now);
            vo = dbf.updateAndRefresh(vo);
            logger.info(String.format("longjob [uuid:%s, name:%s] has been re-submitted", vo.getUuid(), vo.getName()));
        }
        if (vo == null) {
            vo = new LongJobVO();
            if (msg.getResourceUuid() != null) {
                vo.setUuid(msg.getResourceUuid());
            } else {
                vo.setUuid(Platform.getUuid());
            }
            if (msg.getName() != null) {
                vo.setName(msg.getName());
            } else {
                vo.setName(msg.getJobName());
            }
            vo.setDescription(msg.getDescription());
            vo.setApiId(ThreadContext.getImmutableContext().get(Constants.THREAD_CONTEXT_API));
            vo.setJobName(msg.getJobName());
            vo.setJobData(msg.getJobData());
            vo.setState(LongJobState.Waiting);
            vo.setTargetResourceUuid(msg.getTargetResourceUuid());
            vo.setManagementNodeUuid(Platform.getManagementServerId());
            vo.setAccountUuid(msg.getAccountUuid());
            vo = dbf.persistAndRefresh(vo);
            msg.setJobUuid(vo.getUuid());
            tagMgr.createTags(msg.getSystemTags(), msg.getUserTags(), vo.getUuid(), LongJobVO.class.getSimpleName());
            logger.info(String.format("new longjob [uuid:%s, name:%s] has been created", vo.getUuid(), vo.getName()));
        }

        // wait in line
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "longjob-" + msg.getJobUuid();
            }

            @Override
            public void run(SyncTaskChain chain) {
                SubmitLongJobReply reply = new SubmitLongJobReply();
                LongJobVO vo = updateByUuid(msg.getJobUuid(), it -> it.setState(LongJobState.Running));
                // launch the long job right now
                ThreadContext.put(Constants.THREAD_CONTEXT_API, vo.getApiId());
                LongJob job = longJobFactory.getLongJob(vo.getJobName());
                ThreadContext.put(Constants.THREAD_CONTEXT_TASK_NAME, job.getClass().toString());
                doStartJob(job, vo, msg);

                reply.setInventory(LongJobInventory.valueOf(vo));
                if (job.getAuditType() != null) {
                    reply.setNeedAudit(true);
                }
                logger.info(String.format("longjob [uuid:%s, name:%s] has been started", vo.getUuid(), vo.getName()));
                bus.reply(msg, reply);

                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void doStartJob(LongJob job, LongJobVO vo, AsyncBackup async) {
        String longJobUuid = vo.getUuid();
        job.start(vo, new ReturnValueCompletion<APIEvent>(async) {
            @Override
            public void success(APIEvent evt) {
                reportProgress("100");
                updateByUuid(longJobUuid, it -> {
                    it.setState(LongJobState.Succeeded);
                    if (it.getJobResult() == null || it.getJobResult().isEmpty()) {
                        it.setJobResult("Succeeded");
                    }
                });

                exts.forEach(ext -> ext.afterJobFinished(job, vo, evt));
                Optional.ofNullable(longJobCallBacks.remove(vo.getApiId())).ifPresent(it -> it.apply(evt));

                logger.info(String.format("successfully run longjob [uuid:%s, name:%s]", vo.getUuid(), vo.getName()));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                LongJobVO vo = updateByUuid(longJobUuid, it -> {
                    setStateWhenFail(it, errorCode);
                    if (it.getJobResult() == null || it.getJobResult().isEmpty()) {
                        it.setJobResult("Failed : " + errorCode.toString());
                    }
                });

                APIEvent evt = new APIEvent(ThreadContext.get(Constants.THREAD_CONTEXT_API));
                evt.setError(errorCode);

                exts.forEach(ext -> ext.afterJobFailed(job, vo, evt));
                Optional.ofNullable(longJobCallBacks.remove(vo.getApiId())).ifPresent(it -> it.apply(evt));

                logger.info(String.format("failed to run longjob [uuid:%s, name:%s]", vo.getUuid(), vo.getName()));
            }

            private void setStateWhenFail(LongJobVO vo, ErrorCode errorCode) {
                if (Arrays.asList(LongJobState.Canceling, LongJobState.Canceled).contains(vo.getState()) ||
                        errorCode.isError(LongJobErrors.CANCELED)){
                    vo.setState(LongJobState.Canceled);
                } else if (vo.getState() != LongJobState.Suspended) {
                    vo.setState(LongJobState.Failed);
                }
            }
        });
    }

    @Override
    public void submitLongJob(SubmitLongJobMsg msg, CloudBusCallBack submitCallBack, Function<APIEvent, Void> jobCallBack) {
        String apiId = ThreadContext.get(Constants.THREAD_CONTEXT_API);
        longJobCallBacks.put(apiId, jobCallBack);
        bus.makeLocalServiceId(msg, LongJobConstants.SERVICE_ID);
        bus.send(msg, new CloudBusCallBack(submitCallBack) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    longJobCallBacks.remove(apiId);
                }

                if (submitCallBack != null) {
                    submitCallBack.run(reply);
                }
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(LongJobConstants.SERVICE_ID);
    }

    @Override
    public boolean start() {
        collectLongJobs();

        LongJobGlobalConfig.LONG_JOB_DEFAULT_TIMEOUT.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                Long v = Long.valueOf(newValue);
                if (v < 10800) {
                    throw new GlobalConfigException("long job timeout must be larger than 10800s");
                }
            }
        });

        dbf.installEntityLifeCycleCallback(LongJobVO.class, EntityEvent.PRE_UPDATE, (evt, o) -> {
            LongJobVO job = (LongJobVO) o;
            if (job.getExecuteTime() == null && jobCompleted(job)) {
                long time = (System.currentTimeMillis() - job.getCreateDate().getTime()) / 1000;
                job.setExecuteTime(time);
                logger.info(String.format("longjob [uuid:%s] set execute time:%d", job.getUuid(), time));
            }
        });

        populateExtensions();

        return true;
    }

    private void populateExtensions() {
        exts = pluginRgty.getExtensionList(LongJobExtensionPoint.class);
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void nodeJoin(ManagementNodeInventory inv) {

    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
        logger.debug(String.format("Management node[uuid:%s] left, node[uuid:%s] starts to take over longjobs", inv.getUuid(), Platform.getManagementServerId()));
        takeOverLongJob();
    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {

    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {
    }

    private void takeOverLongJob() {
        logger.debug("Starting to take over long jobs");
        final int group = 1000;
        long amount = dbf.count(LongJobVO.class);
        int times = (int) ((amount + group - 1)/group);
        int start = 0;
        for (int i = 0; i < times; i++) {
            List<String> uuids = Q.New(LongJobVO.class)
                    .select(LongJobVO_.uuid)
                    .isNull(LongJobVO_.managementNodeUuid)
                    .limit(group).start(start).listValues();
            for (String uuid : uuids) {
                if (destinationMaker.isManagedByUs(uuid)) {
                    retryTakeOverLongJob(uuid);
                }
            }
            start += group;
        }
    }

    private void retryTakeOverLongJob(String uuid) {
        LongJobOperation operation = null;
        try {
            LongJobVO vo = updateByUuid(uuid, it -> it.setManagementNodeUuid(Platform.getManagementServerId()));
            operation = getLoadOperation(vo);
            doLoadLongJob(vo, operation);
        } catch (Throwable t) {
            if (!(t instanceof SQLNonTransientConnectionException) && isDBConnected()) {
                throw t;
            }

            if (!waitDBConnected(5, 5)) {
                throw t;
            }

            LongJobVO vo = updateByUuid(uuid, it -> it.setManagementNodeUuid(Platform.getManagementServerId()));
            doLoadLongJob(vo, operation);
        }
    }

    @Override
    public void loadLongJob() {
        List<LongJobVO> managedByUsJobs = new SQLBatchWithReturn< List<LongJobVO>>() {
            @Override
            protected List<LongJobVO> scripts() {
                // check long jobs using same uuid with current node
                List<LongJobVO> vos = Q.New(LongJobVO.class)
                        .eq(LongJobVO_.managementNodeUuid, Platform.getManagementServerId())
                        .eq(LongJobVO_.state, LongJobState.Running)
                        .list();
                vos.forEach(vo -> {
                    if (destinationMaker.isManagedByUs(vo.getUuid())) {
                        vo.setJobResult("Failed because management node restarted.");
                        vo.setState(LongJobState.Failed);
                        merge(vo);
                    }
                });

                vos = Q.New(LongJobVO.class).isNull(LongJobVO_.managementNodeUuid).list();
                vos.removeIf(it -> !destinationMaker.isManagedByUs(it.getUuid()));
                vos.forEach(it -> {
                    it.setManagementNodeUuid(Platform.getManagementServerId());
                    merge(it);
                });

                return vos;
            }
        }.execute();

        managedByUsJobs.forEach(this::doLoadLongJob);
    }

    private void doLoadLongJob(LongJobVO vo) {
        doLoadLongJob(vo, null);
    }

    private void doLoadLongJob(LongJobVO vo, LongJobOperation operation) {
        if (operation == null) {
            operation = getLoadOperation(vo);
        }

        if (operation == LongJobOperation.Start) {
            // launch the waiting jobs
            ThreadContext.put(Constants.THREAD_CONTEXT_API, vo.getApiId());
            LongJob job = longJobFactory.getLongJob(vo.getJobName());
            ThreadContext.put(Constants.THREAD_CONTEXT_TASK_NAME, job.getClass().toString());
            doStartJob(job, vo, null);
            SQL.New(LongJobVO.class).eq(LongJobVO_.uuid, vo.getUuid()).set(LongJobVO_.state, LongJobState.Running).update();
        } else if (operation == LongJobOperation.Resume) {
            ThreadContext.put(Constants.THREAD_CONTEXT_API, vo.getApiId());
            LongJob job = longJobFactory.getLongJob(vo.getJobName());
            ThreadContext.put(Constants.THREAD_CONTEXT_TASK_NAME, job.getClass().toString());
            logger.info(String.format("start to resume longjob [uuid:%s, name:%s]", vo.getUuid(), vo.getName()));
            job.resume(vo);
            dbf.update(vo);
        }
    }

    private LongJobOperation getLoadOperation(LongJobVO vo) {
        if (vo.getState() == LongJobState.Waiting) {
            return LongJobOperation.Start;
        } else if (vo.getState() == LongJobState.Running || vo.getState() == LongJobState.Suspended) {
            return LongJobOperation.Resume;
        }
        return null;
    }

    @Override
    public void managementNodeReady() {
        logger.debug(String.format("Management node[uuid:%s] is ready, starts to load longjobs", Platform.getManagementServerId()));
        loadLongJob();
    }

    @Override
    public Long getApiTimeout() {
        String type = ThreadContext.get(Constants.THREAD_CONTEXT_TASK_NAME);
        if (type != null && longJobClasses.contains(type)) {
            Class<? extends APIMessage> batchJobFor = useApiTimeout.get(type);
            if (batchJobFor != null) {
                return getMessageTimeout(batchJobFor);
            }

            // default input unit is second should be changed to millis
            return TimeUnit.SECONDS.toMillis(LongJobGlobalConfig.LONG_JOB_DEFAULT_TIMEOUT.value(Long.class));
        }

        return null;
    }

    private long getMessageTimeout(Class<? extends APIMessage> clz) {
        try {
            return timeoutMgr.getMessageTimeout(clz.newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new CloudRuntimeException(e);
        }
    }
}
