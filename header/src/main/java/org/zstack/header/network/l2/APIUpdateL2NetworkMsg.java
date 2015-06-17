package org.zstack.header.network.l2;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

/**
 * Created by frank on 6/14/2015.
 */
public class APIUpdateL2NetworkMsg extends APIMessage implements L2NetworkMessage {
    @APIParam(resourceType = L2NetworkVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
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
    public String getL2NetworkUuid() {
        return uuid;
    }
}
