package org.zstack.core.scheduler;

import org.zstack.header.core.scheduler.SchedulerVO;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

/**
 * Created by Mei Lei on 8/29/16.
 */
@Action(category = SchedulerConstant.ACTION_CATEGORY)
public class APIResumeSchedulerMsg extends APIMessage implements SchedulerMessage  {
    @APIParam(resourceType = SchedulerVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    @Override
    public String getSchedulerUuid() {
        return uuid;
    }
}
