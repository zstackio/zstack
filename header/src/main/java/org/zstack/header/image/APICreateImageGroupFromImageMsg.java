package org.zstack.header.image;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

import java.util.List;

@Action(category = ImageConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/imagegroup/from/image/{rootVolumeTemplateUuid}",
        method = "POST",
        responseClass = APICreateImageGroupFromImageEvent.class,
        parameterName = "params"
)
@TagResourceType(ImageVO.class)
public class APICreateImageGroupFromImageMsg extends APICreateMessage {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(required = true, maxLength = 2048)
    private String rootVolumeTemplateUuid;
    @APIParam(required = false, maxLength = 2048)
    private String description;
    @APIParam(required = false)
    private List<String> dataVolumeTemplateUuids;

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

    public String getRootVolumeTemplateUuid() {
        return rootVolumeTemplateUuid;
    }

    public void setRootVolumeTemplateUuid(String rootVolumeTemplateUuid) {
        this.rootVolumeTemplateUuid = rootVolumeTemplateUuid;
    }

    public List<String> getDataVolumeTemplateUuids() {
        return dataVolumeTemplateUuids;
    }

    public void setDataVolumeTemplateUuids(List<String> dataVolumeTemplateUuids) {
        this.dataVolumeTemplateUuids = dataVolumeTemplateUuids;
    }

    public static APICreateImageGroupFromImageMsg __example__() {
        APICreateImageGroupFromImageMsg msg = new APICreateImageGroupFromImageMsg();
        msg.setRootVolumeTemplateUuid("b7b9dcad-3c6d-4a7b-9a0a-1b9a20f5c002");
        msg.setName("example-image-group-from-image");
        msg.setDescription("create image group from root image template");
        msg.setDataVolumeTemplateUuids(java.util.Arrays.asList(
                "c1b9dcad-3c6d-4a7b-9a0a-1b9a20f5c003",
                "d2b9dcad-3c6d-4a7b-9a0a-1b9a20f5c004"));
        return msg;
    }
}
