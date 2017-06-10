package org.zstack.scheduler;

import org.springframework.http.HttpMethod;
import org.zstack.header.core.scheduler.APICreateSchedulerJobMessage;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.Map;

/**
 * Created by AlanJager on 2017/6/10.
 */
@Action(category = SchedulerConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/scheduler/jobs",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APICreateSchedulerJobEvent.class
)
public class APICreateSchedulerJobMsg extends APICreateMessage implements APICreateSchedulerJobMessage {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam
    private String targetResourceUuid;
    @APIParam
    private String type;
    @APIParam(required = false)
    private Map<String, String> parameters;

    @Override
    public String getTargetResourceUuid() {
        return this.targetResourceUuid;
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

    public void setTargetResourceUuid(String targetResourceUuid) {
        this.targetResourceUuid = targetResourceUuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
}
