package org.zstack.core.scheduler;

import org.zstack.header.core.scheduler.SchedulerVO;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

/**
 * Created by root on 7/18/16.
 */
@Action(category = SchedulerConstant.ACTION_CATEGORY)
public class APIUpdateSchedulerMsg extends APIMessage implements SchedulerMessage {
    @APIParam(resourceType = SchedulerVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false, emptyString = false)
    private String schedulerName;
    @APIParam(maxLength = 255, required = false, emptyString = false)
    private String schedulerType;
    @APIParam(maxLength = 255, required = false)
    private Integer schedulerInterval;
    @APIParam(maxLength = 255, required = false)
    private Integer repeatCount;
    @APIParam(maxLength = 255, required = false)
    private String cronScheduler;
    @APIParam(required = false)
    private Long startDate;

    public Long getStartDate() {
        return startDate;
    }

    public void setStartDate(Long startDate) {
        this.startDate = startDate;
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

    public Integer getSchedulerInterval() {
        return schedulerInterval;
    }

    public void setSchedulerInterval(Integer schedulerInterval) {
        this.schedulerInterval = schedulerInterval;
    }

    public Integer getRepeatCount() {
        return repeatCount;
    }

    public void setRepeatCount(Integer repeatCount) {
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
        return uuid;
    }


}
