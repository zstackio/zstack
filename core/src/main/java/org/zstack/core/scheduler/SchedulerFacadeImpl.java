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
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.SyncThread;
import org.zstack.header.AbstractService;
import org.zstack.header.core.scheduler.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.identity.AccountResourceRefInventory;
import org.zstack.header.identity.ResourceOwnerPreChangeExtensionPoint;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.Message;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.util.*;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.JobKey.jobKey;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.TriggerKey.triggerKey;

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

    public Scheduler getScheduler() {
        return scheduler;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    protected SchedulerJobVO self;

    public static Map<String, Boolean> taskRunning = new HashMap<String, Boolean>();

    protected SchedulerJobInventory getInventory() {
        return SchedulerJobInventory.valueOf(self);
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIDeleteSchedulerJobMsg) {
            handle((APIDeleteSchedulerJobMsg) msg);
        } else if (msg instanceof APIUpdateSchedulerJobMsg) {
            handle((APIUpdateSchedulerJobMsg) msg);
        } else if (msg instanceof APIChangeSchedulerStateMsg) {
            handle((APIChangeSchedulerStateMsg) msg);
        } else if (msg instanceof APICreateSchedulerTriggerMsg) {
            handle((APICreateSchedulerTriggerMsg) msg);
        } else if (msg instanceof APIUpdateSchedulerTriggerMsg) {
            handle((APIUpdateSchedulerTriggerMsg) msg);
        } else if (msg instanceof APIDeleteSchedulerTriggerMsg) {
            handle((APIDeleteSchedulerTriggerMsg) msg);
        } else if (msg instanceof APIAddSchedulerJobToSchedulerTriggerMsg) {
            handle((APIAddSchedulerJobToSchedulerTriggerMsg) msg);
        } else if (msg instanceof APIRemoveSchedulerJobFromSchedulerTriggerMsg) {
            handle((APIRemoveSchedulerJobFromSchedulerTriggerMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIRemoveSchedulerJobFromSchedulerTriggerMsg msg) {
        APIRemoveSchedulerJobFromSchedulerTriggerEvent evt = new APIRemoveSchedulerJobFromSchedulerTriggerEvent(msg.getId());
        try {
            scheduler.unscheduleJob(triggerKey(msg.getSchedulerTriggerUuid(), msg.getSchedulerTriggerUuid() + "." + msg.getSchedulerJobUuid()));
        } catch (SchedulerException se) {
            throw new RuntimeException(se);
        }

        SQL.New(SchedulerJobSchedulerTriggerRefVO.class)
                .eq(SchedulerJobSchedulerTriggerRefVO_.schedulerJobUuid, msg.getSchedulerJobUuid())
                .eq(SchedulerJobSchedulerTriggerRefVO_.schedulerTriggerUuid, msg.getSchedulerTriggerUuid())
                .delete();

        bus.publish(evt);
    }

    private void handle(APIAddSchedulerJobToSchedulerTriggerMsg msg) {
        APIAddSchedulerJobToSchedulerTriggerEvent evt = new APIAddSchedulerJobToSchedulerTriggerEvent(msg.getId());
        // run scheduler
        SchedulerTask task = prepareSchedulerTaskFromMsg(msg);
        if (runScheduler(task)) {
            SchedulerJobSchedulerTriggerRefVO vo = new SchedulerJobSchedulerTriggerRefVO();
            vo.setUuid(Platform.getUuid());
            vo.setSchedulerJobUuid(msg.getSchedulerJobUuid());
            vo.setSchedulerTriggerUuid(msg.getSchedulerTriggerUuid());
            dbf.persistAndRefresh(vo);
            bus.publish(evt);
        }

    }

    private SchedulerTask prepareSchedulerTaskFromMsg(APIAddSchedulerJobToSchedulerTriggerMsg msg) {
        SchedulerJobVO job = dbf.findByUuid(msg.getSchedulerJobUuid(), SchedulerJobVO.class);
        SchedulerTriggerVO trigger = dbf.findByUuid(msg.getSchedulerTriggerUuid(), SchedulerTriggerVO.class);
        SchedulerTask task = new SchedulerTask();
        task.setStartTime(trigger.getStartTime());
        task.setJobUuid(job.getUuid());
        task.setTargetResourceUuid(job.getTargetResourceUuid());
        task.setJobClassName(job.getJobClassName());
        task.setJobData(job.getJobData());
        task.setTriggerUuid(trigger.getUuid());
        task.setTaskInterval(trigger.getSchedulerInterval());
        task.setTaskRepeatCount(trigger.getRepeatCount());
        task.setType(trigger.getSchedulerType());
        task.setStopTime(trigger.getStopTime());

        return task;
    }

    private void handle(APIDeleteSchedulerTriggerMsg msg) {
        APIDeleteSchedulerTriggerEvent evt = new APIDeleteSchedulerTriggerEvent(msg.getId());
        dbf.removeByPrimaryKey(msg.getUuid(), SchedulerTriggerVO.class);
        bus.publish(evt);
    }

    private void handle(APIUpdateSchedulerTriggerMsg msg) {
        APIUpdateSchedulerTriggerEvent evt = new APIUpdateSchedulerTriggerEvent(msg.getId());
        SchedulerTriggerVO vo = updateSchedulerTrigger(msg);

        if (vo != null) {
            dbf.updateAndRefresh(vo);
        }
        evt.setInventory(SchedulerTriggerInventory.valueOf(vo));
        bus.publish(evt);
    }

    private SchedulerTriggerVO updateSchedulerTrigger(APIUpdateSchedulerTriggerMsg msg) {
        SchedulerTriggerVO vo = dbf.findByUuid(msg.getUuid(), SchedulerTriggerVO.class);

        if (msg.getName() != null) {
            vo.setName(msg.getName());
        }

        if (msg.getDescription() != null) {
            vo.setDescription(msg.getDescription());
        }

        return vo;
    }

    private void handle(APICreateSchedulerTriggerMsg msg) {
        APICreateSchedulerTriggerEvent evt = new APICreateSchedulerTriggerEvent(msg.getId());
        SchedulerTriggerVO vo = new SchedulerTriggerVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        if (msg.getStartTime() == 0) {
            vo.setStartTime(null);
        } else {
            vo.setStartTime(new Timestamp(msg.getStartTime()));
        }

        vo.setRepeatCount(msg.getRepeatCount());
        vo.setSchedulerInterval(msg.getSchedulerInterval());
        vo.setSchedulerType(msg.getSchedulerType());
        dbf.persist(vo);

        evt.setInventory(SchedulerTriggerInventory.valueOf(vo));
        bus.publish(evt);
    }

    private void handle(APIChangeSchedulerStateMsg msg) {
        self = dbf.findByUuid(msg.getSchedulerUuid(), SchedulerJobVO.class);
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

    private void handle(APIDeleteSchedulerJobMsg msg) {
        APIDeleteSchedulerJobEvent evt = new APIDeleteSchedulerJobEvent(msg.getId());
        dbf.removeByPrimaryKey(msg.getUuid(), SchedulerJobVO.class);
        bus.publish(evt);
    }

    private void handle(APIUpdateSchedulerJobMsg msg) {
        SchedulerJobVO vo = updateSchedulerJob(msg);
        if (vo != null) {
            self = dbf.updateAndRefresh(vo);
        }
        APIUpdateSchedulerJobEvent evt = new APIUpdateSchedulerJobEvent(msg.getId());
        evt.setInventory(getInventory());
        bus.publish(evt);
    }

    private SchedulerJobVO updateSchedulerJob(APIUpdateSchedulerJobMsg msg) {
        SchedulerJobVO self = dbf.findByUuid(msg.getSchedulerUuid(), SchedulerJobVO.class);
        if (msg.getName() != null) {
            self.setName(msg.getName());
        }
        if (msg.getDescription() != null) {
            self.setDescription(msg.getDescription());
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
    private void updateSchedulerStatus(String uuid, String state) {
        SQL.New(SchedulerJobSchedulerTriggerRefVO.class)
                .eq(SchedulerJobSchedulerTriggerRefVO_.uuid, uuid)
                .set(SchedulerJobSchedulerTriggerRefVO_.state, state)
                .update();
    }

    public void pauseSchedulerJob(String uuid) {
        logger.debug(String.format("Scheduler %s will change status to Disabled", uuid));
        SchedulerJobSchedulerTriggerRefVO vo = dbf.findByUuid(uuid, SchedulerJobSchedulerTriggerRefVO.class);

        try {
            scheduler.pauseJob(jobKey(vo.getSchedulerJobUuid(), vo.getSchedulerTriggerUuid()));
            updateSchedulerStatus(uuid, SchedulerState.Disabled.toString());
            self = dbf.findByUuid(uuid, SchedulerJobVO.class);
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
//            SimpleQuery<SchedulerVO> q = dbf.createQuery(SchedulerVO.class);
//            q.select(SchedulerVO_.jobName);
//            q.add(SchedulerVO_.uuid, SimpleQuery.Op.EQ, uuid);
//            String jobName = q.findValue();
//            SimpleQuery<SchedulerVO> q2 = dbf.createQuery(SchedulerVO.class);
//            q2.select(SchedulerVO_.jobGroup);
//            q2.add(SchedulerVO_.uuid, SimpleQuery.Op.EQ, uuid);
//            String jobGroup = q2.findValue();
            SchedulerJobSchedulerTriggerRefVO vo = dbf.findByUuid(uuid, SchedulerJobSchedulerTriggerRefVO.class);
            try {
                scheduler.resumeJob(jobKey(vo.getSchedulerJobUuid(), vo.getSchedulerTriggerUuid()));
                updateSchedulerStatus(uuid, SchedulerState.Enabled.toString());
                self = dbf.findByUuid(uuid, SchedulerJobVO.class);
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
//            SimpleQuery<SchedulerVO> q = dbf.createQuery(SchedulerVO.class);
//            q.select(SchedulerVO_.jobName);
//            q.add(SchedulerVO_.uuid, SimpleQuery.Op.EQ, uuid);
//            String jobName = q.findValue();
//            SimpleQuery<SchedulerVO> q2 = dbf.createQuery(SchedulerVO.class);
//            q2.select(SchedulerVO_.jobGroup);
//            q2.add(SchedulerVO_.uuid, SimpleQuery.Op.EQ, uuid);
//            String jobGroup = q2.findValue();
            SchedulerJobSchedulerTriggerRefVO vo = dbf.findByUuid(uuid, SchedulerJobSchedulerTriggerRefVO.class);
            try {
                scheduler.deleteJob(jobKey(vo.getSchedulerJobUuid(), vo.getSchedulerTriggerUuid()));
                dbf.removeByPrimaryKey(uuid, SchedulerJobSchedulerTriggerRefVO.class);
            } catch (SchedulerException e) {
                logger.warn(String.format("Delete Scheduler %s failed!", uuid));
                throw new RuntimeException(e);
            }
        }
    }

    private List<String> getSchedulerManagedByUs() {
        // TODO use scheduler job to find tasks
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
            // TODO use refs to reboot scheduler jobs
            logger.debug(String.format("Scheduler is going to load %s jobs", ours.size()));
            SimpleQuery<SchedulerVO> q = dbf.createQuery(SchedulerVO.class);
            q.add(SchedulerVO_.uuid, SimpleQuery.Op.IN, ours);
            List<SchedulerVO> schedulerRecords = q.list();
            for (SchedulerVO schedulerRecord : schedulerRecords) {
                try {
                    SchedulerJob rebootJob = (SchedulerJob) JSONObjectUtil.toObject(schedulerRecord.getJobData(), Class.forName(schedulerRecord.getJobClassName()));
                    if (schedulerRecord.getState().equals(SchedulerState.Enabled.toString())) {
//                        runScheduler(rebootJob, false);
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

    public boolean runScheduler(SchedulerTask schedulerJob) {
        return runScheduler(schedulerJob, true);
    }

    private boolean runScheduler(SchedulerTask schedulerJob, boolean saveDB) {
        logger.debug(String.format("Starting to generate Scheduler job %s", schedulerJob.getClass().getName()));
        Boolean startNow = false;
        String jobData = schedulerJob.getJobData();
        String jobClassName = schedulerJob.getJobClassName();

        Timestamp start = new Timestamp(System.currentTimeMillis());
        if (schedulerJob.getStartTime() == null && schedulerJob.getType().equals(SchedulerConstant.SIMPLE_TYPE_STRING)) {
            startNow = true;
        }
        try {
            JobDetail job = newJob(SchedulerRunner.class)
                    .withIdentity(schedulerJob.getJobUuid(), schedulerJob.getTriggerUuid())
                    .usingJobData("jobClassName", jobClassName)
                    .usingJobData("jobData", jobData)
                    .build();

            String triggerGroup = schedulerJob.getTriggerUuid() + "." + schedulerJob.getJobUuid();
            String triggerId = schedulerJob.getTriggerUuid();

            // use triggerUuid.jobUuid as the key of a new trigger and jobUuid as group name
            // to support that several jobs use same trigger
            if (schedulerJob.getType().equals("simple")) {
                Trigger trigger;
                if (schedulerJob.getTaskRepeatCount() != null) {
                    if (schedulerJob.getTaskRepeatCount() == 1) {
                        //repeat only once, ignore interval
                        if (startNow) {
                            trigger = newTrigger()
                                    .withIdentity(triggerId, triggerGroup)
                                    .withSchedule(simpleSchedule()
                                    .withMisfireHandlingInstructionNextWithRemainingCount())
                                    .build();
                        } else {
                            trigger = newTrigger()
                                    .withIdentity(triggerId, triggerGroup)
                                    .startAt(schedulerJob.getStartTime())
                                    .withSchedule(simpleSchedule()
                                    .withMisfireHandlingInstructionNextWithRemainingCount())
                                    .build();
                        }

                    } else {
                        //repeat more than once
                        if (startNow) {
                            trigger = newTrigger()
                                    .withIdentity(triggerId, triggerGroup)
                                    .withSchedule(simpleSchedule()
                                            .withIntervalInSeconds(schedulerJob.getTaskInterval())
                                            .withRepeatCount(schedulerJob.getTaskRepeatCount() - 1)
                                            .withMisfireHandlingInstructionNextWithRemainingCount())
                                    .build();
                        } else {
                            trigger = newTrigger()
                                    .withIdentity(triggerId, triggerGroup)
                                    .startAt(schedulerJob.getStartTime())
                                    .withSchedule(simpleSchedule()
                                            .withIntervalInSeconds(schedulerJob.getTaskInterval())
                                            .withRepeatCount(schedulerJob.getTaskRepeatCount() - 1)
                                            .withMisfireHandlingInstructionNextWithRemainingCount())
                                    .build();
                        }
                    }
                } else {
                    if (startNow) {
                        trigger = newTrigger()
                                .withIdentity(triggerId, triggerGroup)
                                .withSchedule(simpleSchedule()
                                        .withIntervalInSeconds(schedulerJob.getTaskInterval())
                                        .repeatForever()
                                        .withMisfireHandlingInstructionNextWithRemainingCount())
                                .build();
                    } else {
                        trigger = newTrigger()
                                .withIdentity(triggerId, triggerGroup)
                                .startAt(schedulerJob.getStartTime())
                                .withSchedule(simpleSchedule()
                                        .withIntervalInSeconds(schedulerJob.getTaskInterval())
                                        .repeatForever()
                                        .withMisfireHandlingInstructionNextWithRemainingCount())
                                .build();
                    }
                }

                scheduler.scheduleJob(job, trigger);
            } else if (schedulerJob.getType().equals(SchedulerConstant.CRON_TYPE_STRING)) {
                CronTrigger trigger = newTrigger()
                        .withIdentity(triggerId, triggerGroup)
                        .withSchedule(cronSchedule(schedulerJob.getCron())
                        .withMisfireHandlingInstructionIgnoreMisfires())
                        .build();
                scheduler.scheduleJob(job, trigger);
            }
        } catch (SchedulerException se) {
            logger.warn(String.format("Run Scheduler task by job[uuid:%s] and trigger[uuid:%s] failed", schedulerJob.getJobUuid(), schedulerJob.getTriggerUuid()));
            throw new RuntimeException(se);
        }

        return true;
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
