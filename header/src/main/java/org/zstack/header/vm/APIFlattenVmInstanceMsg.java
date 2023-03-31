package org.zstack.header.vm;


import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageVO;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 36)
@RestRequest(path = "/vm-instances/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIFlattenVmInstanceEvent.class,
        isAction = true
)
public class APIFlattenVmInstanceMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String uuid;

    @APIParam(required = false)
    private boolean full = true;

    @APIParam(required = false)
    private boolean dryRun;

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setFull(boolean full) {
        this.full = full;
    }

    public boolean isFull() {
        return full;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }
}
