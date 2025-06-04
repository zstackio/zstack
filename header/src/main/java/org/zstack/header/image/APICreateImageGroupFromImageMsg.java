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
        method = HttpMethod.POST,
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
    private List<String> dateVolumeTemplateUuids;

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

    public List<String> getDateVolumeTemplateUuids() {
        return dateVolumeTemplateUuids;
    }

    public void setDateVolumeTemplateUuids(List<String> dateVolumeTemplateUuids) {
        this.dateVolumeTemplateUuids = dateVolumeTemplateUuids;
    }
}
