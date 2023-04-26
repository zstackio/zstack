package org.zstack.header.volume;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;

import java.util.concurrent.TimeUnit;

@Action(category = VolumeConstant.ACTION_CATEGORY)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 36)
@RestRequest(
        path = "/volumes/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIFlattenVolumeEvent.class
)
public class APIFlattenVolumeMsg extends APIMessage implements VolumeMessage {
    @APIParam(resourceType = VolumeVO.class, checkAccount = true, operationTarget = true)
    private String uuid;

    @APIParam(required = false)
    private boolean dryRun;

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    @Override
    public String getVolumeUuid() {
        return uuid;
    }
}
