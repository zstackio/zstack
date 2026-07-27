package org.zstack.networksecuritypolicyschedule;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.List;

@Action(category = NetworkSecurityPolicyScheduleConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/network-security-policy-schedules/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateNetworkSecurityPolicyScheduleEvent.class,
        isAction = true
)
public class APIUpdateNetworkSecurityPolicyScheduleMsg extends APIMessage {
    @APIParam(resourceType = NetworkSecurityPolicyScheduleVO.class)
    private String uuid;

    @APIParam(maxLength = 255, emptyString = false)
    private String name;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(validValues = {"Local", "UTC"})
    private String timeType;

    @APIParam(validValues = {"Once", "Weekly"})
    private String repeatType;

    @APIParam
    private String startDate;

    @APIParam
    private String endDate;

    @APIParam
    private String startTime;

    @APIParam
    private String endTime;

    @APIParam(required = false)
    private List<Integer> weekDays;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public String getTimeType() {
        return timeType;
    }

    public void setTimeType(String timeType) {
        this.timeType = timeType;
    }

    public String getRepeatType() {
        return repeatType;
    }

    public void setRepeatType(String repeatType) {
        this.repeatType = repeatType;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public List<Integer> getWeekDays() {
        return weekDays;
    }

    public void setWeekDays(List<Integer> weekDays) {
        this.weekDays = weekDays;
    }

}
