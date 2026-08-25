package org.zstack.networksecuritypolicyschedule;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vo.ResourceVO;

@Action(category = NetworkSecurityPolicyScheduleConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/network-security-policy-schedules",
        method = HttpMethod.GET,
        responseClass = APIGetNetworkSecurityPolicyScheduleReply.class
)
public class APIGetNetworkSecurityPolicyScheduleMsg extends APISyncCallMessage {
    @APIParam(resourceType = ResourceVO.class, checkAccount = true)
    private String resourceUuid;

    @APIParam(required = false, validValues = {"NotStarted", "InWindow", "OutOfWindow", "Ended"})
    private String timeStatus;

    @APIParam(required = false, validValues = {"Once", "Weekly"})
    private String repeatType;

    @APIParam(required = false, validValues = {"Local", "UTC"})
    private String timeType;

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getTimeStatus() {
        return timeStatus;
    }

    public void setTimeStatus(String timeStatus) {
        this.timeStatus = timeStatus;
    }

    public String getRepeatType() {
        return repeatType;
    }

    public void setRepeatType(String repeatType) {
        this.repeatType = repeatType;
    }

    public String getTimeType() {
        return timeType;
    }

    public void setTimeType(String timeType) {
        this.timeType = timeType;
    }

    public static APIGetNetworkSecurityPolicyScheduleMsg __example__() {
        APIGetNetworkSecurityPolicyScheduleMsg msg =
                new APIGetNetworkSecurityPolicyScheduleMsg();
        msg.setResourceUuid(uuid());
        msg.setTimeStatus(NetworkSecurityPolicyScheduleTimeStatus.OutOfWindow.name());
        msg.setRepeatType(NetworkSecurityPolicyScheduleRepeatType.Weekly.name());
        msg.setTimeType(NetworkSecurityPolicyScheduleTimeType.UTC.name());
        return msg;
    }
}
