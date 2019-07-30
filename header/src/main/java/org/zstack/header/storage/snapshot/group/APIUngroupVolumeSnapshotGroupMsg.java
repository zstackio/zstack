package org.zstack.header.storage.snapshot.group;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.snapshot.VolumeSnapshotConstant;

/**
 * Created by MaJin on 2019/7/9.
 */
@Action(category = VolumeSnapshotConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/volume-snapshots/ungroup/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIUngroupVolumeSnapshotGroupEvent.class
)
public class APIUngroupVolumeSnapshotGroupMsg extends APIMessage implements VolumeSnapshotGroupMessage {
    @APIParam(resourceType = VolumeSnapshotGroupVO.class, successIfResourceNotExisting = true)
    private String uuid;

    @Override
    public String getGroupUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
