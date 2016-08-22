package org.zstack.core.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.header.core.scheduler.APICreateSchedulerMessage;

import java.sql.Timestamp;
import java.util.Date;

/**
 * Created by root on 8/3/16.
 */

public class AbstractSchedulerJob implements SchedulerJob {
    @Autowired
    protected transient CloudBus bus;

    private Date startDate;
    private Integer interval;
    private Integer repeat;
    private String type;
    private String cron;
    private String schedulerName;
    private String schedulerDescription;
    private String jobName;
    private String jobGroup;
    private String triggerGroup;
    private String triggerName;
    private String resourceUuid;
    private String targetResourceUuid;
    private Timestamp createDate;

    public AbstractSchedulerJob() {
    }

    public AbstractSchedulerJob(APICreateSchedulerMessage msg) {
        String jobIdentifyUuid = Platform.getUuid();
        Date date = new Date();
        createDate = new Timestamp(date.getTime());
        schedulerName = msg.getSchedulerName();
        type = msg.getType();
        resourceUuid = msg.getResourceUuid();

        if ( msg.getSchedulerDescription() != null && ! msg.getSchedulerDescription().isEmpty()) {
            schedulerDescription = msg.getSchedulerDescription();
        }

        if ( msg.getCron() != null && ! msg.getCron().isEmpty()) {
            cron = msg.getCron();
        }

        if ( msg.getStartTime() != null) {
            startDate = new Date(msg.getStartTime() * 1000);
        }

        if ( msg.getInterval() != null) {
            interval = msg.getInterval();
        }

        if ( msg.getRepeatCount() != null) {
            repeat = msg.getRepeatCount();
        }

        // jobName, jobGroup, triggerName, triggerGroup reserved for future API
        if(msg.getJobName() != null && !msg.getJobName().isEmpty()) {
            jobName = msg.getJobName();
        }
        else {
            jobName = jobIdentifyUuid;
        }

        if(msg.getJobGroup() != null && !msg.getJobGroup().isEmpty()) {
            jobGroup = msg.getJobGroup();
        }
        else {
            jobGroup = jobIdentifyUuid;
        }

        if(msg.getTriggerName() != null && !msg.getTriggerName().isEmpty()) {
            triggerName = msg.getTriggerName();
        }
        else {
            triggerName = jobIdentifyUuid;
        }

        if (msg.getTriggerGroup() != null && !msg.getTriggerGroup().isEmpty()) {
            triggerGroup = msg.getTriggerGroup();
        }
        else {
            triggerGroup = jobIdentifyUuid;
        }
    }

    public String getSchedulerDescription() {
        return schedulerDescription;
    }

    public void setSchedulerDescription(String schedulerDescription) {
        this.schedulerDescription = schedulerDescription;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Integer getInterval() {
        return interval;
    }

    public Integer getSchedulerInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    public Integer getRepeat() {
        return repeat;
    }

    public void setRepeat(Integer repeat) {
        this.repeat = repeat;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getSchedulerName() {
        return schedulerName;
    }

    public void setSchedulerName(String schedulerName) {
        this.schedulerName = schedulerName;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getJobGroup() {
        return jobGroup;
    }

    public void setJobGroup(String jobGroup) {
        this.jobGroup = jobGroup;
    }

    public String getTriggerGroup() {
        return triggerGroup;
    }

    public void setTriggerGroup(String triggerGroup) {
        this.triggerGroup = triggerGroup;
    }

    public String getTriggerName() {
        return triggerName;
    }

    public void setTriggerName(String triggerName) {
        this.triggerName = triggerName;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {

        this.createDate = createDate;
    }

    public String getTargetResourceUuid() {
        return targetResourceUuid;
    }

    public void setTargetResourceUuid(String targetResourceUuid) {
        this.targetResourceUuid = targetResourceUuid;
    }

    @Override
    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public void run() {}
}
