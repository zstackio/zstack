package org.zstack.core.scheduler;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.Message;
import org.zstack.utils.gson.JSONObjectUtil;

import java.sql.Date;
import java.sql.Timestamp;
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
public class SchedulerFacadeImpl extends AbstractService implements SchedulerFacade {
    @Autowired
    private transient CloudBus bus;
    @Autowired
    private transient ErrorFacade errf;
    @Autowired
    protected transient DatabaseFacade dbf;

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
            e.printStackTrace();
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
            if ( msg.getStartTimeStamp() != 0 ) {
                self.setStartDate(new Timestamp(msg.getStartTimeStamp()));
                reSchedule = true;
            }
            Trigger oldTrigger = null;
            try {
                oldTrigger = scheduler.getTrigger(triggerKey(triggerName, triggerGroup));
            } catch (SchedulerException e) {
                e.printStackTrace();
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
                e.printStackTrace();
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
                e.printStackTrace();
            }
            TriggerBuilder tb = oldTrigger.getTriggerBuilder();
            Trigger newTrigger = tb.withSchedule(cronSchedule(msg.getCronScheduler()))
                    .build();
            try {
                scheduler.rescheduleJob(oldTrigger.getKey(), newTrigger);
                update = true;
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
        }

        return update ? self : null;
    }


    public String getId() {
        return bus.makeLocalServiceId(SchedulerConstant.SERVICE_ID);
    }

    public boolean start() {
        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
        List<SchedulerVO> schedulerRecords = dbf.listAll(SchedulerVO.class);
        Iterator<SchedulerVO> schedulerRecordsIterator = schedulerRecords.iterator();
        while (schedulerRecordsIterator.hasNext()) {
            SchedulerVO schedulerRecord = schedulerRecordsIterator.next();
            try {
                SchedulerJob rebootJob = (SchedulerJob) JSONObjectUtil.toObject(schedulerRecord.getJobData(), Class.forName(schedulerRecord.getJobClassName()));
                schedulerRunner(rebootJob, false);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    public boolean stop() {
        try {
            scheduler.shutdown();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void schedulerRunner(SchedulerJob schedulerJob) {
        schedulerRunner(schedulerJob, true);
    }

    private void schedulerRunner(SchedulerJob schedulerJob, boolean saveDB) {
        Timestamp start = null;
        SchedulerVO vo = new SchedulerVO();
        Timestamp create = new Timestamp(System.currentTimeMillis());
        if ( schedulerJob.getStartDate() != null ) {
            start = new Timestamp(schedulerJob.getStartDate().getTime());
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
            vo.setUuid(Platform.getUuid());
            vo.setSchedulerType(schedulerJob.getType());
            vo.setSchedulerName(schedulerJob.getSchedulerName());
            vo.setCreateDate(create);
            vo.setJobName(schedulerJob.getJobName());
            vo.setJobGroup(schedulerJob.getJobGroup());
            vo.setTriggerName(schedulerJob.getTriggerName());
            vo.setTriggerGroup(schedulerJob.getTriggerGroup());
            vo.setJobClassName(jobClassName);
        }

        try {

            JobDetail job = newJob(SchedulerRunner.class)
                    .withIdentity(schedulerJob.getJobName(), schedulerJob.getJobGroup())
                    .usingJobData("jobClassName", jobClassName)
                    .usingJobData("jobData", jobData)
                    .build();
            if ( schedulerJob.getType().equals("simple")) {
                Trigger trigger = null;
                if ( schedulerJob.getRepeat() != 0 ) {
                    trigger = newTrigger()
                            .withIdentity(schedulerJob.getTriggerName(), schedulerJob.getTriggerGroup())
                            .startAt(schedulerJob.getStartDate())
                            .withSchedule(simpleSchedule()
                                    .withIntervalInSeconds(schedulerJob.getSchedulerInterval())
                                    .withRepeatCount(schedulerJob.getRepeat()))
                            .build();
                }
                else {
                    trigger = newTrigger()
                            .withIdentity(schedulerJob.getTriggerName(), schedulerJob.getTriggerGroup())
                            .startAt(schedulerJob.getStartDate())
                            .withSchedule(simpleSchedule()
                                    .withIntervalInSeconds(schedulerJob.getSchedulerInterval())
                                    .repeatForever())
                            .build();
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
            se.printStackTrace();
        }

        if (saveDB) {
            vo.setStatus(SchedulerStatus.Enabled.toString());
            dbf.persist(vo);
        }
    }
}
