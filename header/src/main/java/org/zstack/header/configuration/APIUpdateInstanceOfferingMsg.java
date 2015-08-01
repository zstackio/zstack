package org.zstack.header.configuration;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

/**
 * Created by frank on 6/15/2015.
 */
@Action(category = ConfigurationConstant.ACTION_CATEGORY)
public class APIUpdateInstanceOfferingMsg extends APIMessage implements InstanceOfferingMessage {
    @APIParam(resourceType = InstanceOfferingVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam(required = false, maxLength = 255)
    private String name;
    @APIParam(required = false, maxLength = 2048)
    private String description;

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

    @Override
    public String getInstanceOfferingUuid() {
        return uuid;
    }
}
