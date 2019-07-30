package org.zstack.header.storage.snapshot.group;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.snapshot.VolumeSnapshotConstant;

import java.util.concurrent.TimeUnit;

/**
 * Created by MaJin on 2019/7/9.
 */
@Action(category = VolumeSnapshotConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/volume-snapshots/group/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteVolumeSnapshotGroupEvent.class
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 3)
public class APIDeleteVolumeSnapshotGroupMsg extends APIDeleteMessage implements VolumeSnapshotGroupMessage {
    @APIParam(resourceType = VolumeSnapshotGroupVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getGroupUuid() {
        return uuid;
    }
}
