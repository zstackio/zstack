package org.zstack.core.scheduler;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.SyncThread;
import org.zstack.header.AbstractService;
import org.zstack.header.core.scheduler.SchedulerInventory;
import org.zstack.header.core.scheduler.SchedulerState;
import org.zstack.header.core.scheduler.SchedulerVO;
import org.zstack.header.core.scheduler.SchedulerVO_;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.identity.AccountResourceRefInventory;
import org.zstack.header.identity.ResourceOwnerPreChangeExtensionPoint;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.Message;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import java.sql.Timestamp;
import java.util.*;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.JobKey.jobKey;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Created by Mei Lei on 6/22/16.
 */
public class SchedulerFacadeImpl extends AbstractService implements SchedulerFacade, ManagementNodeReadyExtensionPoint,
        ManagementNodeChangeListener, ResourceOwnerPreChangeExtensionPoint, VmStateChangedExtensionPoint,
        VmBeforeExpungeExtensionPoint, VmInstanceDestroyExtensionPoint, RecoverVmExtensionPoint {
    private static final CLogger logger = Utils.getLogger(SchedulerFacadeImpl.class);

    @Autowired
    private transient CloudBus bus;
    @Autowired
    private transient ErrorFacade errf;
    @Autowired
    protected transient DatabaseFacade dbf;
    @Autowired
    private transient ResourceDestinationMaker destinationMaker;

    private Scheduler scheduler;

    protected SchedulerVO self;

    public static Map<String, Boolean> taskRunning = new HashMap<String, Boolean>();

    protected SchedulerInventory getInventory() {
        return SchedulerInventory.valueOf(self);
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIDeleteSchedulerMsg) {
            handle((APIDeleteSchedulerMsg) msg);
        } else if (msg instanceof APIUpdateSchedulerMsg) {
            handle((APIUpdateSchedulerMsg) msg);
        } else if (msg instanceof APIChangeSchedulerStateMsg) {
            handle((APIChangeSchedulerStateMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIChangeSchedulerStateMsg msg) {
        self = dbf.findByUuid(msg.getSchedulerUuid(), SchedulerVO.class);
        if (msg.getStateEvent().equals("enable")) {
            resumeSchedulerJob(msg.getUuid());
            APIChangeSchedulerStateEvent evt = new APIChangeSchedulerStateEvent(msg.getId());
            evt.setInventory(getInventory());
            bus.publish(evt);
        } else {
            pauseSchedulerJob(msg.getUuid());
            APIChangeSchedulerStateEvent evt = new APIChangeSchedulerStateEvent(msg.getId());
            evt.setInventory(getInventory());
            bus.publish(evt);
        }

    }

    private void handle(APIDeleteSchedulerMsg msg) {
        APIDeleteSchedulerEvent evt = new APIDeleteSchedulerEvent(msg.getId());
        SimpleQuery<SchedulerVO> q = dbf.createQuery(SchedulerVO.class);
        q.select(SchedulerVO_.jobName);
        q.add(SchedulerVO_.uuid, SimpleQuery.Op.EQ, msg.getUuid());
        String jobName = q.findValue();
        SimpleQuery<SchedulerVO> q2 = dbf.createQuery(SchedulerVO.class);
        q2.select(SchedulerVO_.jobGroup);
        q2.add(SchedulerVO_.uuid, SimpleQuery.Op.EQ, msg.getUuid());
        String jobGroup = q2.findValue();
        try {
            scheduler.deleteJob(jobKey(jobName, jobGroup));
            dbf.removeByPrimaryKey(msg.getUuid(), SchedulerVO.class);
            bus.publish(evt);
        } catch (SchedulerException e) {
            evt.setErrorCode(errf.instantiateErrorCode(SysErrors.DELETE_RESOURCE_ERROR, e.getMessage()));
            bus.publish(evt);
            logger.warn(String.format("Delete Scheduler %s failed!", msg.getUuid()));
            throw new RuntimeException(e);
        }

    }

    private void handle(APIUpdateSchedulerMsg msg) {
        SchedulerVO vo = updateScheduler(msg);
        if (vo != null) {
            self = dbf.updateAndRefresh(vo);
        }
        APIUpdateSchedulerEvent evt = new APIUpdateSchedulerEvent(msg.getId());
        evt.setInventory(getInventory());
        bus.publish(evt);
    }

    private SchedulerVO updateScheduler(APIUpdateSchedulerMsg msg) {
        SchedulerVO self = dbf.findByUuid(msg.getSchedulerUuid(), SchedulerVO.class);
        if (msg.getSchedulerName() != null) {
            self.setSchedulerName(msg.getSchedulerName());
        }
        if (msg.getSchedulerDescription() != null) {
            self.setSchedulerDescription(msg.getSchedulerDescription());
        }
        return self;
    }


    public String getId() {

        return bus.makeLocalServiceId(SchedulerConstant.SERVICE_ID);
    }

    public boolean start() {
        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
        } catch (SchedulerException e) {
            logger.warn("Start Scheduler failed!");
            throw new RuntimeException(e);
        }

        return true;
    }

    public boolean stop() {
        try {
            scheduler.shutdown();
        } catch (SchedulerException e) {
            logger.warn("Stop Scheduler failed!");
            throw new RuntimeException(e);
        }
        return true;
    }

    @Transactional
    private void updateSchedulerStatus(String uuid, String status) {
        String sql = "update SchedulerVO scheduler set scheduler.state= :state where scheduler.uuid = :schedulerUuid";
        Query q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("state", status);
        q.setParameter("schedulerUuid", uuid);
        q.executeUpdate();
    }


    public void pauseSchedulerJob(String uuid) {
        logger.debug(String.format("Scheduler %s will change status to Disabled", uuid));
        SimpleQuery<SchedulerVO> q = dbf.createQuery(SchedulerVO.class);
        q.select(SchedulerVO_.jobName);
        q.add(SchedulerVO_.uuid, SimpleQuery.Op.EQ, uuid);
        String jobName = q.findValue();
        SimpleQuery<SchedulerVO> q2 = dbf.createQuery(SchedulerVO.class);
        q2.select(SchedulerVO_.jobGroup);
        q2.add(SchedulerVO_.uuid, SimpleQuery.Op.EQ, uuid);
        String jobGroup = q2.findValue();
        try {
            scheduler.pauseJob(jobKey(jobName, jobGroup));
            updateSchedulerStatus(uuid, SchedulerState.Disabled.toString());
            self = dbf.findByUuid(uuid, SchedulerVO.class);
        } catch (SchedulerException e) {
            logger.warn(String.format("Pause Scheduler %s failed!", uuid));
            throw new RuntimeException(e);
        }
    }

    public void resumeSchedulerJob(String uuid) {
        if (!destinationMaker.isManagedByUs(uuid)) {
            logger.debug(String.format("Scheduler %s not managed by us, will not be resume", uuid));
        } else {
            logger.debug(String.format("Scheduler %s will change status to Enabled", uuid));
            SimpleQuery<SchedulerVO> q = dbf.createQuery(SchedulerVO.class);
            q.select(SchedulerVO_.jobName);
            q.add(SchedulerVO_.uuid, SimpleQuery.Op.EQ, uuid);
            String jobName = q.findValue();
            SimpleQuery<SchedulerVO> q2 = dbf.createQuery(SchedulerVO.class);
            q2.select(SchedulerVO_.jobGroup);
            q2.add(SchedulerVO_.uuid, SimpleQuery.Op.EQ, uuid);
            String jobGroup = q2.findValue();
            try {
                scheduler.resumeJob(jobKey(jobName, jobGroup));
                updateSchedulerStatus(uuid, SchedulerState.Enabled.toString());
                self = dbf.findByUuid(uuid, SchedulerVO.class);
            } catch (SchedulerException e) {
                logger.warn(String.format("Resume Scheduler %s failed!", uuid));
                throw new RuntimeException(e);
            }

        }
    }

    public void deleteSchedulerJob(String uuid) {
        if (!destinationMaker.isManagedByUs(uuid)) {
            logger.debug(String.format("Scheduler %s not managed by us, will not be deleted", uuid));
        } else {
            logger.debug(String.format("Scheduler %s will be deleted", uuid));
            SimpleQuery<SchedulerVO> q = dbf.createQuery(SchedulerVO.class);
            q.select(SchedulerVO_.jobName);
            q.add(SchedulerVO_.uuid, SimpleQuery.Op.EQ, uuid);
            String jobName = q.findValue();
            SimpleQuery<SchedulerVO> q2 = dbf.createQuery(SchedulerVO.class);
            q2.select(SchedulerVO_.jobGroup);
            q2.add(SchedulerVO_.uuid, SimpleQuery.Op.EQ, uuid);
            String jobGroup = q2.findValue();
            try {
                scheduler.deleteJob(jobKey(jobName, jobGroup));
                dbf.removeByPrimaryKey(uuid, SchedulerVO.class);
            } catch (SchedulerException e) {
                logger.warn(String.format("Delete Scheduler %s failed!", uuid));
                throw new RuntimeException(e);
            }
        }
    }

    private List<String> getSchedulerManagedByUs() {
        SimpleQuery<SchedulerVO> q = dbf.createQuery(SchedulerVO.class);
        q.select(SchedulerVO_.uuid);
        q.add(SchedulerVO_.managementNodeUuid, SimpleQuery.Op.NULL);
        List<String> ids = q.listValue();

        List<String> ours = new ArrayList<String>();
        for (String id : ids) {
            if (destinationMaker.isManagedByUs(id)) {
                ours.add(id);
                q = dbf.createQuery(SchedulerVO.class);
                q.add(SchedulerVO_.uuid, SimpleQuery.Op.IN, ours);
                List<SchedulerVO> vos = q.list();
                for (SchedulerVO vo : vos) {
                    vo.setManagementNodeUuid(Platform.getManagementServerId());
                    dbf.updateAndRefresh(vo);
                }
            }
        }
        return ours;
    }

    private void jobLoader(List<String> ours) {
        if (ours.isEmpty()) {
            logger.debug("no Scheduler managed by us");
        } else {
            logger.debug(String.format("Scheduler is going to load %s jobs", ours.size()));
            SimpleQuery<SchedulerVO> q = dbf.createQuery(SchedulerVO.class);
            q.add(SchedulerVO_.uuid, SimpleQuery.Op.IN, ours);
            List<SchedulerVO> schedulerRecords = q.list();
            for (SchedulerVO schedulerRecord : schedulerRecords) {
                try {
                    SchedulerJob rebootJob = (SchedulerJob) JSONObjectUtil.toObject(schedulerRecord.getJobData(), Class.forName(schedulerRecord.getJobClassName()));
                    if (schedulerRecord.getState().equals(SchedulerState.Enabled.toString())) {
                        runScheduler(rebootJob, false);
                    }
                } catch (ClassNotFoundException e) {
                    logger.warn(String.format("Load Scheduler %s failed!", schedulerRecord.getUuid()));
                    throw new RuntimeException(e);
                }
            }
        }

    }

    private void loadSchedulerJobs() {
        List<String> ours = getSchedulerManagedByUs();
        jobLoader(ours);
    }

    private void takeOverScheduler() {
        logger.debug(String.format("Starting to take over Scheduler job "));
        int qun = 10000;
        long amount = dbf.count(SchedulerVO.class);
        int times = (int) (amount / qun) + (amount % qun != 0 ? 1 : 0);
        int start = 0;
        for (int i = 0; i < times; i++) {
            SimpleQuery<SchedulerVO> q = dbf.createQuery(SchedulerVO.class);
            q.select(SchedulerVO_.uuid);
            q.add(SchedulerVO_.managementNodeUuid, SimpleQuery.Op.NULL);
            q.setLimit(qun);
            q.setStart(start);
            List<String> uuids = q.listValue();
            List<String> ours = new ArrayList<String>();
            for (String id : uuids) {
                if (destinationMaker.isManagedByUs(id)) {
                    ours.add(id);
                    q = dbf.createQuery(SchedulerVO.class);
                    q.add(SchedulerVO_.uuid, SimpleQuery.Op.IN, ours);
                    List<SchedulerVO> vos = q.list();
                    for (SchedulerVO vo : vos) {
                        vo.setManagementNodeUuid(Platform.getManagementServerId());
                        dbf.updateAndRefresh(vo);
                    }
                }
            }
            jobLoader(ours);
            start += qun;
        }
    }

    public SchedulerVO runScheduler(SchedulerJob schedulerJob) {
        return runScheduler(schedulerJob, true);
    }

    private SchedulerVO runScheduler(SchedulerJob schedulerJob, boolean saveDB) {
        logger.debug(String.format("Starting to generate Scheduler job %s", schedulerJob.getClass().getName()));
        Timestamp start = null;
        Boolean startNow = false;
        SchedulerVO vo = new SchedulerVO();
        Timestamp create = new Timestamp(System.currentTimeMillis());
        if (schedulerJob.getStartTime() != null) {
            if (!schedulerJob.getStartTime().equals(new Date(0))) {
                start = new Timestamp(schedulerJob.getStartTime().getTime());
            } else {
                startNow = true;
                start = create;
            }
        }
        String jobData = JSONObjectUtil.toJsonString(schedulerJob);
        String jobClassName = schedulerJob.getClass().getName();
        if (saveDB) {
            if (schedulerJob.getType().equals("simple")) {
                vo.setRepeatCount(schedulerJob.getRepeat());
                vo.setSchedulerInterval(schedulerJob.getSchedulerInterval());
                if (schedulerJob.getRepeat() != null) {
                    if (schedulerJob.getRepeat() == 1) {
                        vo.setStopTime(start);
                    } else {
                        vo.setStopTime(new Timestamp(schedulerJob.getStartTime().getTime() + schedulerJob.getRepeat() * schedulerJob.getSchedulerInterval() * 1000));
                    }
                } else {
                    vo.setStopTime(null);
                }
                vo.setStartTime(start);
            } else if (schedulerJob.getType().equals("cron")) {
                vo.setCronScheduler(schedulerJob.getCron());
                vo.setStopTime(null);
            } else {
                logger.error(String.format("Unknown scheduler job type %s", schedulerJob.getType()));
            }

            vo.setJobData(jobData);

            if (schedulerJob.getResourceUuid() != null) {
                vo.setUuid(schedulerJob.getResourceUuid());
            } else {
                vo.setUuid(Platform.getUuid());
            }
            vo.setSchedulerJob(jobClassName.substring(jobClassName.lastIndexOf(".") + 1));
            vo.setSchedulerType(schedulerJob.getType());
            vo.setSchedulerName(schedulerJob.getSchedulerName());
            vo.setCreateDate(create);
            vo.setJobName(schedulerJob.getJobName());
            vo.setJobGroup(schedulerJob.getJobGroup());
            vo.setTriggerName(schedulerJob.getTriggerName());
            vo.setTriggerGroup(schedulerJob.getTriggerGroup());
            vo.setJobClassName(jobClassName);
            vo.setManagementNodeUuid(Platform.getManagementServerId());
            vo.setTargetResourceUuid(schedulerJob.getTargetResourceUuid());
        }

        try {

            JobDetail job = newJob(SchedulerRunner.class)
                    .withIdentity(schedulerJob.getJobName(), schedulerJob.getJobGroup())
                    .usingJobData("jobClassName", jobClassName)
                    .usingJobData("jobData", jobData)
                    .build();
            if (schedulerJob.getType().equals("simple")) {
                Trigger trigger;
                if (schedulerJob.getRepeat() != null) {
                    if (schedulerJob.getRepeat() == 1) {
                        //repeat only once, ignore interval
                        if (startNow) {
                            trigger = newTrigger()
                                    .withIdentity(schedulerJob.getTriggerName(), schedulerJob.getTriggerGroup())
                                    .withSchedule(simpleSchedule()
                                    .withMisfireHandlingInstructionNextWithRemainingCount())
                                    .build();
                        } else {
                            trigger = newTrigger()
                                    .withIdentity(schedulerJob.getTriggerName(), schedulerJob.getTriggerGroup())
                                    .startAt(schedulerJob.getStartTime())
                                    .withSchedule(simpleSchedule()
                                    .withMisfireHandlingInstructionNextWithRemainingCount())
                                    .build();
                        }

                    } else {
                        //repeat more than once
                        if (startNow) {
                            trigger = newTrigger()
                                    .withIdentity(schedulerJob.getTriggerName(), schedulerJob.getTriggerGroup())
                                    .withSchedule(simpleSchedule()
                                            .withIntervalInSeconds(schedulerJob.getSchedulerInterval())
                                            .withRepeatCount(schedulerJob.getRepeat() - 1)
                                            .withMisfireHandlingInstructionNextWithRemainingCount())
                                    .build();
                        } else {
                            trigger = newTrigger()
                                    .withIdentity(schedulerJob.getTriggerName(), schedulerJob.getTriggerGroup())
                                    .startAt(schedulerJob.getStartTime())
                                    .withSchedule(simpleSchedule()
                                            .withIntervalInSeconds(schedulerJob.getSchedulerInterval())
                                            .withRepeatCount(schedulerJob.getRepeat() - 1)
                                            .withMisfireHandlingInstructionNextWithRemainingCount())
                                    .build();
                        }
                    }
                } else {
                    if (startNow) {
                        trigger = newTrigger()
                                .withIdentity(schedulerJob.getTriggerName(), schedulerJob.getTriggerGroup())
                                .withSchedule(simpleSchedule()
                                        .withIntervalInSeconds(schedulerJob.getSchedulerInterval())
                                        .repeatForever()
                                        .withMisfireHandlingInstructionNextWithRemainingCount())
                                .build();
                    } else {
                        trigger = newTrigger()
                                .withIdentity(schedulerJob.getTriggerName(), schedulerJob.getTriggerGroup())
                                .startAt(schedulerJob.getStartTime())
                                .withSchedule(simpleSchedule()
                                        .withIntervalInSeconds(schedulerJob.getSchedulerInterval())
                                        .repeatForever()
                                        .withMisfireHandlingInstructionNextWithRemainingCount())
                                .build();
                    }
                }

                scheduler.scheduleJob(job, trigger);
            } else if (schedulerJob.getType().equals("cron")) {
                CronTrigger trigger = newTrigger()
                        .withIdentity(schedulerJob.getTriggerName(), schedulerJob.getTriggerGroup())
                        .withSchedule(cronSchedule(schedulerJob.getCron())
                        .withMisfireHandlingInstructionIgnoreMisfires())
                        .build();
                scheduler.scheduleJob(job, trigger);
            }
        } catch (SchedulerException se) {
            logger.warn(String.format("Run Scheduler  %s failed", vo.getUuid()));
            throw new RuntimeException(se);
        }

        if (saveDB) {
            logger.debug(String.format("Save Scheduler job %s to database", schedulerJob.getClass().getName()));
            vo.setState(SchedulerState.Enabled.toString());
            dbf.persist(vo);
            return vo;
        }
        return null;
    }

    @AsyncThread
    @Override
    public void managementNodeReady() {
        logger.debug(String.format("Management node[uuid:%s] joins, start loading Scheduler jobs...", Platform.getManagementServerId()));
        loadSchedulerJobs();
    }

    @Override
    public void nodeJoin(String nodeId) {

    }

    @Override
    @SyncThread
    public void nodeLeft(String nodeId) {
        logger.debug(String.format("Management node[uuid:%s] left, node[uuid:%s] starts to take over schedulers", nodeId, Platform.getManagementServerId()));
        takeOverScheduler();
    }

    @Override
    public void iAmDead(String nodeId) {

    }

    @Override
    public void iJoin(String nodeId) {

    }

    @Override
    public void resourceOwnerPreChange(AccountResourceRefInventory ref, String newOwnerUuid) {
        SimpleQuery<SchedulerVO> q = dbf.createQuery(SchedulerVO.class);
        q.select(SchedulerVO_.uuid);
        q.add(SchedulerVO_.targetResourceUuid, SimpleQuery.Op.EQ, ref.getResourceUuid());
        List<String> uuids = q.listValue();
        if (!uuids.isEmpty()) {
            for (String uuid : uuids) {
                if (!destinationMaker.isManagedByUs(uuid)) {
                    logger.debug(String.format("Scheduler %s not managed by us, will not to pause it", uuid));
                } else {
                    logger.debug(String.format("resource %s: %s scheduler %s will be paused",
                            ref.getResourceType(), ref.getResourceUuid(), uuid));
                    pauseSchedulerJob(uuid);
                }
            }
        } else {
            logger.debug(String.format("resource %s: %s not set any scheduler", ref.getResourceType(), ref.getResourceUuid()));
        }
    }

    public void vmStateChanged(VmInstanceInventory vm, VmInstanceState oldState, VmInstanceState newState) {
        SimpleQuery<SchedulerVO> q = dbf.createQuery(SchedulerVO.class);
        q.select(SchedulerVO_.uuid);
        q.add(SchedulerVO_.targetResourceUuid, SimpleQuery.Op.EQ, vm.getUuid());
        List<String> uuids = q.listValue();
        if (!uuids.isEmpty()) {
            for (String uuid : uuids) {
                if (oldState.toString().equals("Running") && newState.toString().equals("Unknown")) {
                    pauseSchedulerJob(uuid);
                } else if (oldState.toString().equals("Unknown") && newState.toString().equals("Running")) {
                    resumeSchedulerJob(uuid);
                }
            }
        } else {
            logger.debug(String.format("vm %s not set any scheduler", vm.getUuid()));
        }
    }


    public String preDestroyVm(VmInstanceInventory inv) {
        return null;
    }

    public void beforeDestroyVm(VmInstanceInventory inv) {
        logger.debug(String.format("will pause scheduler before destroy vm %s", inv.getUuid()));
        SimpleQuery<SchedulerVO> q = dbf.createQuery(SchedulerVO.class);
        q.add(SchedulerVO_.targetResourceUuid, SimpleQuery.Op.EQ, inv.getUuid());
        q.select(SchedulerVO_.uuid);
        List<String> uuids = q.listValue();
        for (String uuid : uuids) {
            pauseSchedulerJob(uuid);
        }

    }

    public void afterDestroyVm(VmInstanceInventory vm) {

    }

    public void failedToDestroyVm(VmInstanceInventory vm, ErrorCode reason) {

    }

    public void preRecoverVm(VmInstanceInventory vm) {

    }

    public void beforeRecoverVm(VmInstanceInventory vm) {

    }

    public void afterRecoverVm(VmInstanceInventory vm) {

    }

    public void vmBeforeExpunge(VmInstanceInventory inv) {
        logger.debug(String.format("will delete scheduler before expunge vm"));
        SimpleQuery<SchedulerVO> q = dbf.createQuery(SchedulerVO.class);
        q.add(SchedulerVO_.targetResourceUuid, SimpleQuery.Op.EQ, inv.getUuid());
        q.select(SchedulerVO_.uuid);
        List<String> uuids = q.listValue();
        for (String uuid : uuids) {
            deleteSchedulerJob(uuid);
        }
    }
}
