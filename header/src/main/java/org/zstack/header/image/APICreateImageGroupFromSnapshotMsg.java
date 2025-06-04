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
        path = "/imagegroup/from/snapshot/{rootVolumeSnapshotUuid}",
        method = HttpMethod.POST,
        responseClass = APICreateImageGroupFromSnapshotEvent.class,
        parameterName = "params"
)
@TagResourceType(ImageVO.class)
public class APICreateImageGroupFromSnapshotMsg extends APICreateMessage {
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(required = true, maxLength = 2048)
    private String rootVolumeSnapshotUuid;
    @APIParam(required = false, maxLength = 2048)
    private String description;
    @APIParam(required = false)
    private List<String> dateVolumeSnapshotUuids;

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

    public String getRootVolumeSnapshotUuid() {
        return rootVolumeSnapshotUuid;
    }

    public void setRootVolumeSnapshotUuid(String rootVolumeSnapshotUuid) {
        this.rootVolumeSnapshotUuid = rootVolumeSnapshotUuid;
    }

    public List<String> getDateVolumeSnapshotUuids() {
        return dateVolumeSnapshotUuids;
    }

    public void setDateVolumeSnapshotUuids(List<String> dateVolumeSnapshotUuids) {
        this.dateVolumeSnapshotUuids = dateVolumeSnapshotUuids;
    }
}
