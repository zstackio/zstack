package org.zstack.header.image;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;
import org.zstack.header.vm.VmInstanceVO;

@Action(category = ImageConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/images/groups/from/vm-instance",
        method = "POST",
        responseClass = APICreateImageGroupFromVmInstanceEvent.class,
        parameterName = "params"
)
@TagResourceType(ImageVO.class)
public class APICreateImageGroupFromVmInstanceMsg extends APICreateMessage {
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String vmInstanceUuid;
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(required = false, maxLength = 2048)
    private String description;

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
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

    public static APICreateImageGroupFromVmInstanceMsg __example__() {
        APICreateImageGroupFromVmInstanceMsg msg = new APICreateImageGroupFromVmInstanceMsg();
        msg.setVmInstanceUuid("e7b9dcad-3c6d-4a7b-9a0a-1b9a20f5c001");
        msg.setName("example-image-group-from-vm");
        msg.setDescription("create image group from vm instance");
        return msg;
    }
}
