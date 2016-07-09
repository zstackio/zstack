package org.zstack.core.scheduler;

import org.zstack.header.identity.AccountMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

import java.sql.Timestamp;

/**
 * Created by root on 7/18/16.
 */
public class APIUpdateSchedulerMsg extends APIMessage implements SchedulerMessage{
    @APIParam(resourceType = SchedulerVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false, emptyString = false)
    private String schedulerName;
    @APIParam(maxLength = 255, required = false, emptyString = false)
    private String schedulerType;
    @APIParam(maxLength = 255, required = false)
    private int schedulerInterval;
    @APIParam(maxLength = 255, required = false)
    private int repeatCount;
    @APIParam(maxLength = 255, required = false)
    private String cronScheduler;
    @APIParam(required = false)
    private long startTimeStamp;

    public long getStartTimeStamp() {
        return startTimeStamp;
    }

    public void setStartTimeStamp(long startTimeStamp) {
        this.startTimeStamp = startTimeStamp;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getSchedulerName() {
        return schedulerName;
    }

    public void setSchedulerName(String schedulerName) {
        this.schedulerName = schedulerName;
    }

    public String getSchedulerType() {
        return schedulerType;
    }

    public void setSchedulerType(String schedulerType) {
        this.schedulerType = schedulerType;
    }

    public int getSchedulerInterval() {
        return schedulerInterval;
    }

    public void setSchedulerInterval(int schedulerInterval) {
        this.schedulerInterval = schedulerInterval;
    }

    public int getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(int repeatCount) {
        this.repeatCount = repeatCount;
    }

    public String getCronScheduler() {
        return cronScheduler;
    }

    public void setCronScheduler(String cronScheduler) {
        this.cronScheduler = cronScheduler;
    }


    @Override
    public String getSchedulerUuid() {
       return  uuid;
    }


}
