package org.zstack.header.configuration;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 2:35 PM
 * To change this template use File | Settings | File Templates.
 */
@Action(category = ConfigurationConstant.ACTION_CATEGORY)
public class APIChangeDiskOfferingStateMsg extends APIMessage implements DiskOfferingMessage {
    @APIParam(resourceType = DiskOfferingVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam(validValues = {"enable", "disable"})
    private String stateEvent;

    @Override
    public String getDiskOfferingUuid() {
        return getUuid();
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
}
