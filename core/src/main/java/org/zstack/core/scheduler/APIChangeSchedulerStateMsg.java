package org.zstack.core.scheduler;

import org.zstack.header.core.scheduler.SchedulerVO;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

/**
 * Created by Mei Lei on 8/31/16.
 */
@Action(category = SchedulerConstant.ACTION_CATEGORY)
public class APIChangeSchedulerStateMsg  extends APIMessage implements SchedulerMessage  {
    @APIParam(resourceType = SchedulerVO.class)
    private String uuid;
    @APIParam(validValues={"enable", "disable"})
    private String stateEvent;

    public APIChangeSchedulerStateMsg() {
    }

    public APIChangeSchedulerStateMsg(String uuid, String stateEvent) {
        this.uuid = uuid;
        this.stateEvent = stateEvent;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getStateEvent() {
        return stateEvent;
    }

    public void setStateEvent(String stateEvent) {
        this.stateEvent = stateEvent;
    }

    @Override
    public String getSchedulerUuid() {
        return uuid;
    }
}
