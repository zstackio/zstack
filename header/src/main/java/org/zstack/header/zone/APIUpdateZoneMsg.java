package org.zstack.header.zone;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

/**
 * Created by frank on 6/14/2015.
 */
public class APIUpdateZoneMsg extends APIMessage implements ZoneMessage {
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(resourceType = ZoneVO.class)
    private String uuid;

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
    public String getZoneUuid() {
        return uuid;
    }
}
