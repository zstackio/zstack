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
    @APIParam(maxLength = 2048, required = false)
    private String schedulerDescription;

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

    public String getSchedulerDescription() {
        return schedulerDescription;
    }

    public void setSchedulerDescription(String schedulerDescription) {
        this.schedulerDescription = schedulerDescription;
    }

    @Override
    public String getSchedulerUuid() {
        return uuid;
    }


}
