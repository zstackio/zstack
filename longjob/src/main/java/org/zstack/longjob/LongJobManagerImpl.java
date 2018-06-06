package org.zstack.longjob;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.config.GlobalConfigValidatorExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.timeout.ApiTimeoutExtensionPoint;
import org.zstack.header.AbstractService;
import org.zstack.header.Constants;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.identity.APIDeleteAccountEvent;
import org.zstack.header.longjob.*;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.identity.AccountManager;
import org.zstack.tag.TagManager;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.progress.ProgressReportService.reportProgress;

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
    private AccountManager acntMgr;
    @Autowired
    private transient ResourceDestinationMaker destinationMaker;

    // we need a longjob factory to produce LongJob based on JobName
    @Autowired
    private LongJobFactory longJobFactory;

    private List<String> longJobClasses = new ArrayList<String>();

    private void collectLongJobs() {
        Set<Class<?>> subs = BeanUtils.reflections.getTypesAnnotatedWith(LongJobFor.class);
        for (Class sub : subs) {
            longJobClasses.add(sub.toString());
        }
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APISubmitLongJobMsg) {
            handle((APISubmitLongJobMsg) msg);
        } else if (msg instanceof APICancelLongJobMsg) {
            handle((APICancelLongJobMsg) msg);
        } else if (msg instanceof APIDeleteLongJobMsg) {
            handle((APIDeleteLongJobMsg) msg);
        } else if (msg instanceof SubmitLongJobMsg) {
            handle((SubmitLongJobMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
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
                LongJobVO vo = dbf.findByUuid(msg.getUuid(), LongJobVO.class);
                LongJob job = longJobFactory.getLongJob(vo.getJobName());
                job.cancel(vo, new Completion(msg) {
                    LongJobVO vo = dbf.findByUuid(msg.getUuid(), LongJobVO.class);

                    @Override
                    public void success() {
                        vo.setState(LongJobState.Canceled);
                        dbf.update(vo);
                        logger.info(String.format("longjob [uuid:%s, name:%s] has been canceled", vo.getUuid(), vo.getName()));
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        vo.setState(LongJobState.Failed);
                        dbf.update(vo);
                        logger.error(String.format("failed to cancel longjob [uuid:%s, name:%s]", vo.getUuid(), vo.getName()));
                    }
                });

                vo.setState(LongJobState.Canceling);
                dbf.update(vo);
                logger.info(String.format("longjob [uuid:%s, name:%s] has been marked canceling", vo.getUuid(), vo.getName()));
                bus.publish(evt);

                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
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
                bus.publish(evt);
            }
        });
    }

    private void handle(SubmitLongJobMsg msg) {
        // create LongJobVO
        LongJobVO vo = new LongJobVO();
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

        // wait in line
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return "longjob-" + msg.getJobUuid();
            }

            @Override
            public void run(SyncTaskChain chain) {
                SubmitLongJobReply reply = new SubmitLongJobReply();
                LongJobVO vo = dbf.findByUuid(msg.getJobUuid(), LongJobVO.class);
                vo.setState(LongJobState.Running);
                vo = dbf.updateAndRefresh(vo);
                // launch the long job right now
                ThreadContext.put(Constants.THREAD_CONTEXT_API, vo.getApiId());
                LongJob job = longJobFactory.getLongJob(vo.getJobName());
                ThreadContext.put(Constants.THREAD_CONTEXT_TASK_NAME, job.getClass().toString());
                job.start(vo, new Completion(msg) {
                    LongJobVO vo = dbf.findByUuid(msg.getJobUuid(), LongJobVO.class);

                    @Override
                    public void success() {
                        reportProgress("100");
                        vo = dbf.reload(vo);
                        vo.setState(LongJobState.Succeeded);
                        if (vo.getJobResult() == null || vo.getJobResult().isEmpty())
                            vo.setJobResult("Succeeded");
                        dbf.update(vo);
                        logger.info(String.format("successfully run longjob [uuid:%s, name:%s]", vo.getUuid(), vo.getName()));
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        vo = dbf.reload(vo);
                        vo.setState(LongJobState.Failed);
                        if (vo.getJobResult() == null || vo.getJobResult().isEmpty())
                            vo.setJobResult("Failed : " + errorCode.toString());
                        dbf.update(vo);
                        logger.info(String.format("failed to run longjob [uuid:%s, name:%s]", vo.getUuid(), vo.getName()));
                    }
                });

                reply.setInventory(LongJobInventory.valueOf(vo));
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

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void nodeJoin(String nodeId) {

    }

    @Override
    public void nodeLeft(String nodeId) {
        logger.debug(String.format("Management node[uuid:%s] left, node[uuid:%s] starts to take over longjobs", nodeId, Platform.getManagementServerId()));
        takeOverLongJob();
    }

    @Override
    public void iAmDead(String nodeId) {

    }

    @Override
    public void iJoin(String nodeId) {

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
                    LongJobVO vo = dbf.findByUuid(uuid, LongJobVO.class);
                    vo.setManagementNodeUuid(Platform.getManagementServerId());
                    dbf.updateAndRefresh(vo);
                }
            }
            start += group;
        }
    }

    @Override
    public void loadLongJob() {
        new SQLBatch() {
            @Override
            protected void scripts() {
                List<LongJobVO> vos = Q.New(LongJobVO.class).isNull(LongJobVO_.managementNodeUuid).list();
                if (vos.isEmpty()) {
                    return;
                }

                for (LongJobVO vo : vos) {
                    if (destinationMaker.isManagedByUs(vo.getUuid())) {
                        vo.setManagementNodeUuid(Platform.getManagementServerId());
                        vo = doLoadLongJob(vo);
                        merge(vo);
                    }
                }
            }
        }.execute();
    }

    private LongJobVO doLoadLongJob(LongJobVO vo) {
        if (vo.getState() == LongJobState.Waiting) {
            // launch the waiting jobs
            ThreadContext.put(Constants.THREAD_CONTEXT_API, vo.getApiId());
            LongJob job = longJobFactory.getLongJob(vo.getJobName());
            ThreadContext.put(Constants.THREAD_CONTEXT_TASK_NAME, job.getClass().toString());
            job.start(vo, new Completion(null) {
                @Override
                public void success() {
                    reportProgress("100");
                    vo.setState(LongJobState.Succeeded);
                    dbf.update(vo);
                    logger.info(String.format("successfully run longjob [uuid:%s, name:%s]", vo.getUuid(), vo.getName()));
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    vo.setState(LongJobState.Failed);
                    dbf.update(vo);
                    logger.info(String.format("failed to run longjob [uuid:%s, name:%s]", vo.getUuid(), vo.getName()));
                }
            });

            vo.setState(LongJobState.Running);
        } else if (vo.getState() == LongJobState.Running) {
            // set running jobs to error
            vo.setJobResult("Failed because management node restarted.");
            vo.setState(LongJobState.Failed);
        }

        return vo;
    }

    @Override
    public void managementNodeReady() {
        logger.debug(String.format("Management node[uuid:%s] is ready, starts to load longjobs", Platform.getManagementServerId()));
        loadLongJob();
    }

    @Override
    public Long getApiTimeout(Class claz) {
        String type = ThreadContext.get(Constants.THREAD_CONTEXT_TASK_NAME);
        if (type != null && longJobClasses.contains(type)) {
            // default input unit is second should be changed to millis
            return TimeUnit.SECONDS.toMillis(LongJobGlobalConfig.LONG_JOB_DEFAULT_TIMEOUT.value(Long.class));
        }

        return null;
    }
}
