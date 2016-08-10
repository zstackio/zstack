package org.zstack.core.scheduler;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.Message;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.JobKey.jobKey;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.TriggerKey.triggerKey;

/**
 * Created by Mei Lei on 6/22/16.
 */
public class SchedulerFacadeImpl extends AbstractService implements SchedulerFacade, ManagementNodeReadyExtensionPoint {
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

    protected SchedulerInventory getInventory() {
        return SchedulerInventory.valueOf(self);
    }
    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIDeleteSchedulerMsg) {
            handle((APIDeleteSchedulerMsg) msg);
        } else  if (msg instanceof  APIUpdateSchedulerMsg) {
            handle((APIUpdateSchedulerMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
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
            logger.warn("Delete Scheduler trigger failed!");
            throw new RuntimeException(e);
        }

    }

    private void handle(APIUpdateSchedulerMsg msg) {
        if (msg.getSchedulerType() == null) {
            throw new ApiMessageInterceptionException(errf.instantiateErrorCode(SysErrors.INVALID_ARGUMENT_ERROR,
                    String.format("schedulerType must be set")
            ));
        }
        SchedulerVO vo = updateScheduler(msg);
        if (vo != null) {
            self = dbf.updateAndRefresh(vo);
        }
        APIUpdateSchedulerEvent evt = new APIUpdateSchedulerEvent(msg.getId());
        evt.setInventory(getInventory());
        bus.publish(evt);
    }

    private SchedulerVO updateScheduler(APIUpdateSchedulerMsg msg) {
        boolean update = false;
        boolean reSchedule = false;
        self = dbf.findByUuid(msg.getUuid(), SchedulerVO.class);
        SimpleQuery<SchedulerVO> q = dbf.createQuery(SchedulerVO.class);
        q.select(SchedulerVO_.triggerName);
        q.add(SchedulerVO_.uuid, SimpleQuery.Op.EQ, msg.getUuid());
        String triggerName = q.findValue();
        SimpleQuery<SchedulerVO> q2 = dbf.createQuery(SchedulerVO.class);
        q2.select(SchedulerVO_.triggerGroup);
        q2.add(SchedulerVO_.uuid, SimpleQuery.Op.EQ, msg.getUuid());
        String triggerGroup = q2.findValue();
        if (msg.getSchedulerName() != null ) {
            self.setSchedulerName(msg.getSchedulerName());
        }
        if (msg.getSchedulerType().equals("simple")) {
            if (msg.getSchedulerInterval() != 0 ) {
                self.setSchedulerInterval(msg.getSchedulerInterval());
                reSchedule = true;
            }
            if (msg.getRepeatCount() != 0 ) {
                self.setRepeatCount(msg.getRepeatCount());
                reSchedule = true;
            }
            if ( msg.getStartDate() != 0 ) {
                self.setStartDate(new Timestamp(msg.getStartDate()));
                reSchedule = true;
            }
            Trigger oldTrigger = null;
            try {
                oldTrigger = scheduler.getTrigger(triggerKey(triggerName, triggerGroup));
            } catch (SchedulerException e) {
                logger.warn("Get Scheduler trigger failed!");
                throw new RuntimeException(e);
            }
            TriggerBuilder tb = oldTrigger != null ? oldTrigger.getTriggerBuilder() : null;
            Trigger newTrigger = null;
            if (tb != null && reSchedule) {
                if ( msg.getRepeatCount() != 0 ) {
                    newTrigger = tb.withSchedule(simpleSchedule()
                            .withIntervalInSeconds(msg.getSchedulerInterval())
                            .withRepeatCount(msg.getRepeatCount()))
                            .build();
                }
                else {
                    newTrigger = tb.withSchedule(simpleSchedule()
                            .withIntervalInSeconds(msg.getSchedulerInterval())
                            .repeatForever())
                            .build();
                }
            }

            try {
                scheduler.rescheduleJob(oldTrigger != null ? oldTrigger.getKey() : null, newTrigger);
                update = true;
            } catch (SchedulerException e) {
                logger.warn("Reschedule simple Scheduler job failed!");
                throw new RuntimeException(e);
            }

        }
        else if (msg.getSchedulerType().equals("cron")) {
            if ( msg.getCronScheduler() != null ) {
                self.setCronScheduler(msg.getCronScheduler());
            }
            // retrieve the trigger
            Trigger oldTrigger = null;
            try {
                oldTrigger = scheduler.getTrigger(triggerKey(triggerName, triggerGroup));
            } catch (SchedulerException e) {
                logger.warn("Get Scheduler trigger failed!");
                throw new RuntimeException(e);
            }
            TriggerBuilder tb = oldTrigger.getTriggerBuilder();
            Trigger newTrigger = tb.withSchedule(cronSchedule(msg.getCronScheduler()))
                    .build();
            try {
                scheduler.rescheduleJob(oldTrigger.getKey(), newTrigger);
                update = true;
            } catch (SchedulerException e) {
                logger.warn("Reschedule cron Scheduler job failed!");
                throw new RuntimeException(e);
            }
        }

        return update ? self : null;
    }


    public String getId() {

        return bus.makeLocalServiceId(SchedulerConstant.SERVICE_ID);
    }

    private void loadSchedulerJobs() {

        SimpleQuery<SchedulerVO> q = dbf.createQuery(SchedulerVO.class);
        q.select(SchedulerVO_.uuid);
        q.add(SchedulerVO_.managementNodeUuid, SimpleQuery.Op.NULL);
        List<String> ids = q.listValue();

        List<String> ours = new ArrayList<String>();
        for (String id : ids) {
            if (destinationMaker.isManagedByUs(id)) {
                ours.add(id);
            }
        }

        if (ours.isEmpty()) {
            logger.debug("no Scheduler managed by us");
        } else {
            logger.debug(String.format("Scheduler is going to load %s jobs", ours.size()));
            q = dbf.createQuery(SchedulerVO.class);
            q.add(SchedulerVO_.uuid, SimpleQuery.Op.IN, ours);
            List<SchedulerVO> schedulerRecords = q.list();

            Iterator<SchedulerVO> schedulerRecordsIterator = schedulerRecords.iterator();
            while (schedulerRecordsIterator.hasNext()) {
                SchedulerVO schedulerRecord = schedulerRecordsIterator.next();
                try {
                    SchedulerJob rebootJob = (SchedulerJob) JSONObjectUtil.toObject(schedulerRecord.getJobData(), Class.forName(schedulerRecord.getJobClassName()));
                    runScheduler(rebootJob, false);
                } catch (ClassNotFoundException e) {
                    logger.warn("Load Scheduler job failed!");
                    throw new RuntimeException(e);
                }
            }
        }
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

    @AsyncThread
    @Override
    public void managementNodeReady() {
        logger.debug(String.format("Management node[uuid:%s] joins, start loading Scheduler jobs...", Platform.getManagementServerId()));
        loadSchedulerJobs();
    }

    public void runScheduler(SchedulerJob schedulerJob) {
        runScheduler(schedulerJob, true);
    }

    private void runScheduler(SchedulerJob schedulerJob, boolean saveDB) {
        logger.debug(String.format("Starting to run Scheduler job %s", schedulerJob.getClass().getName()));
        Timestamp start = null;
        Boolean startNow = false;
        SchedulerVO vo = new SchedulerVO();
        Timestamp create = new Timestamp(System.currentTimeMillis());
        if ( schedulerJob.getStartDate() != null ) {
            if ( ! schedulerJob.getStartDate().equals(new Date(0))) {
                start = new Timestamp(schedulerJob.getStartDate().getTime());
            } else {
                startNow = true;
                start = create;
            }
        }
        String jobData = JSONObjectUtil.toJsonString(schedulerJob);
        String jobClassName = schedulerJob.getClass().getName();
        if (saveDB) {
            if ( start != null) {
                vo.setStartDate(start);
            }

            if ( schedulerJob.getSchedulerInterval() != 0 ) {
                vo.setSchedulerInterval(schedulerJob.getSchedulerInterval());
            }

            if ( schedulerJob.getCron() != null ) {
                vo.setCronScheduler(schedulerJob.getCron());
            }

            if ( schedulerJob.getRepeat() != 0 ) {
                vo.setRepeatCount(schedulerJob.getRepeat());
            }
            vo.setJobData(jobData);

            if (schedulerJob.getResourceUuid() != null) {
                vo.setUuid(schedulerJob.getResourceUuid());
            } else {
                vo.setUuid(Platform.getUuid());
            }

            vo.setSchedulerType(schedulerJob.getType());
            vo.setSchedulerName(schedulerJob.getSchedulerName());
            vo.setCreateDate(create);
            vo.setJobName(schedulerJob.getJobName());
            vo.setJobGroup(schedulerJob.getJobGroup());
            vo.setTriggerName(schedulerJob.getTriggerName());
            vo.setTriggerGroup(schedulerJob.getTriggerGroup());
            vo.setJobClassName(jobClassName);
            vo.setManagementNodeUuid(Platform.getManagementServerId());
        }

        try {

            JobDetail job = newJob(SchedulerRunner.class)
                    .withIdentity(schedulerJob.getJobName(), schedulerJob.getJobGroup())
                    .usingJobData("jobClassName", jobClassName)
                    .usingJobData("jobData", jobData)
                    .build();
            if ( schedulerJob.getType().equals("simple")) {
                Trigger trigger;
                if ( schedulerJob.getRepeat() != 0 ) {
                    if ( schedulerJob.getRepeat() == 1) {
                        if ( startNow ) {
                            trigger = newTrigger()
                                    .withIdentity(schedulerJob.getTriggerName(), schedulerJob.getTriggerGroup())
                                    .withSchedule(simpleSchedule())
                                    .build();
                        } else {
                            trigger = newTrigger()
                                    .withIdentity(schedulerJob.getTriggerName(), schedulerJob.getTriggerGroup())
                                    .startAt(schedulerJob.getStartDate())
                                    .withSchedule(simpleSchedule())
                                    .build();
                        }

                    } else {
                        if ( startNow ) {
                            trigger = newTrigger()
                                    .withIdentity(schedulerJob.getTriggerName(), schedulerJob.getTriggerGroup())
                                    .withSchedule(simpleSchedule()
                                            .withIntervalInSeconds(schedulerJob.getSchedulerInterval())
                                            .withRepeatCount(schedulerJob.getRepeat() - 1))
                                    .build();
                        } else {
                            trigger = newTrigger()
                                    .withIdentity(schedulerJob.getTriggerName(), schedulerJob.getTriggerGroup())
                                    .startAt(schedulerJob.getStartDate())
                                    .withSchedule(simpleSchedule()
                                            .withIntervalInSeconds(schedulerJob.getSchedulerInterval())
                                            .withRepeatCount(schedulerJob.getRepeat() - 1))
                                    .build();
                        }
                    }
                }
                else {
                    if ( startNow ) {
                        trigger = newTrigger()
                                .withIdentity(schedulerJob.getTriggerName(), schedulerJob.getTriggerGroup())
                                .withSchedule(simpleSchedule()
                                        .withIntervalInSeconds(schedulerJob.getSchedulerInterval())
                                        .repeatForever())
                                .build();
                    } else {
                        trigger = newTrigger()
                                .withIdentity(schedulerJob.getTriggerName(), schedulerJob.getTriggerGroup())
                                .startAt(schedulerJob.getStartDate())
                                .withSchedule(simpleSchedule()
                                        .withIntervalInSeconds(schedulerJob.getSchedulerInterval())
                                        .repeatForever())
                                .build();
                    }
                }
                scheduler.scheduleJob(job, trigger);
            }
            else if (schedulerJob.getType().equals("cron")){
                CronTrigger trigger = newTrigger()
                        .withIdentity(schedulerJob.getTriggerName(), schedulerJob.getTriggerGroup())
                        .withSchedule(cronSchedule(schedulerJob.getCron()))
                        .build();
                scheduler.scheduleJob(job, trigger);
            }
        } catch (SchedulerException se) {
            logger.warn("Run Scheduler failed!");
            throw new RuntimeException(se);
        }

        if (saveDB) {
            vo.setStatus(SchedulerStatus.Enabled.toString());
            dbf.persist(vo);
        }
    }
}
