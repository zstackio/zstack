package org.zstack.core.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.header.core.scheduler.SchedulerTriggerVO;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

import java.sql.Timestamp;

/**
 * Created by AlanJager on 2017/6/7.
 */

@TagResourceType(SchedulerTriggerVO.class)
@Action(category = SchedulerConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/scheduler/trigger",
        method = HttpMethod.POST,
        responseClass = APICreateSchedulerTriggerEvent.class,
        parameterName = "params"
)
public class APICreateSchedulerTriggerMsg extends APICreateMessage {
    @APIParam(required = true, maxLength = 255)
    private String name;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(required = false)
    private Integer schedulerInterval;

    @APIParam(required = false)
    private Integer repeatCount;

    @APIParam(required = false)
    private Timestamp startTime;

    @APIParam(required = true, validValues = {"simple", "cron"})
    private String schedulerType;

    public String getSchedulerType() {
        return schedulerType;
    }

    public void setSchedulerType(String schedulerType) {
        this.schedulerType = schedulerType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }
}
