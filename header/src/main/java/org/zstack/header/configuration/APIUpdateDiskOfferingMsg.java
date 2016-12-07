package org.zstack.header.configuration;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 6/15/2015.
 */
@Action(category = ConfigurationConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/disk-offerings/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIUpdateDiskOfferingEvent.class,
        parameterName = "params"
)
public class APIUpdateDiskOfferingMsg extends APIMessage implements DiskOfferingMessage {
    @APIParam(resourceType = DiskOfferingVO.class, checkAccount = true, operationTarget = true)
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
    public String getDiskOfferingUuid() {
        return uuid;
    }
}
