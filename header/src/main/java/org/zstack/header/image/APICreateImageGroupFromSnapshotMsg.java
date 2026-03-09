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
        method = "POST",
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
    private List<String> dataVolumeSnapshotUuids;

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

    public List<String> getDataVolumeSnapshotUuids() {
        return dataVolumeSnapshotUuids;
    }

    public void setDataVolumeSnapshotUuids(List<String> dataVolumeSnapshotUuids) {
        this.dataVolumeSnapshotUuids = dataVolumeSnapshotUuids;
    }

    public static APICreateImageGroupFromSnapshotMsg __example__() {
        APICreateImageGroupFromSnapshotMsg msg = new APICreateImageGroupFromSnapshotMsg();
        msg.setRootVolumeSnapshotUuid("aa12b3cd-3c6d-4a7b-9a0a-1b9a20f5c005");
        msg.setName("example-image-group-from-snapshot");
        msg.setDescription("create image group from root volume snapshot");
        msg.setDataVolumeSnapshotUuids(java.util.Arrays.asList(
                "bb12b3cd-3c6d-4a7b-9a0a-1b9a20f5c006",
                "cc12b3cd-3c6d-4a7b-9a0a-1b9a20f5c007"
        ));
        return msg;
    }
}
