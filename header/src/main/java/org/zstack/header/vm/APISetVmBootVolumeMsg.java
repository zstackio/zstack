package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.volume.VolumeVO;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by MaJin on 2021/1/7.
 */

@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISetVmBootVolumeEvent.class
)
public class APISetVmBootVolumeMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String vmInstanceUuid;
    @APIParam(resourceType = VolumeVO.class, checkAccount = true, operationTarget = true)
    private String volumeUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public static APISetVmBootVolumeMsg __example__() {
        APISetVmBootVolumeMsg msg = new APISetVmBootVolumeMsg();
        msg.vmInstanceUuid = uuid();
        msg.volumeUuid = uuid();
        return msg;
    }
}
