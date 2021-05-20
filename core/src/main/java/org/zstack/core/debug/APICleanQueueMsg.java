package org.zstack.core.debug;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.SuppressCredentialCheck;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by LiangHanYu on 2021/5/20 14:51
 */
@SuppressCredentialCheck
@RestRequest(
        path = "/clean/queue",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APICleanQueueEvent.class)
public class APICleanQueueMsg extends APISyncCallMessage {
    @APIParam()
    private String signatureName;

    @APIParam()
    private Integer taskIndex;

    @APIParam(required = false)
    private Boolean isCleanUp = false;

    @APIParam(required = false)
    private Boolean isRunningTask = true;

    public String getSignatureName() {
        return signatureName;
    }

    public void setSignatureName(String signatureName) {
        this.signatureName = signatureName;
    }

    public Integer getTaskIndex() {
        return taskIndex;
    }

    public void setTaskIndex(Integer taskIndex) {
        this.taskIndex = taskIndex;
    }

    public Boolean getCleanUp() {
        return isCleanUp;
    }

    public void setCleanUp(Boolean cleanUp) {
        isCleanUp = cleanUp;
    }

    public Boolean getRunningTask() {
        return isRunningTask;
    }

    public void setRunningTask(Boolean runningTask) {
        isRunningTask = runningTask;
    }

    public static APICleanQueueMsg __example__() {
        APICleanQueueMsg msg = new APICleanQueueMsg();
        msg.taskIndex = 1;
        msg.signatureName = "ping-kvm-host-7c5a200899b842af990dcaebf72c885b";
        msg.isCleanUp = false;
        msg.isRunningTask = true;
        return msg;
    }

}
