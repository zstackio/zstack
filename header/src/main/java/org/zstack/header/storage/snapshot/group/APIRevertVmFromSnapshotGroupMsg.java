package org.zstack.header.storage.snapshot.group;

/**
 * Created by MaJin on 2019/7/9.
 */

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.snapshot.VolumeSnapshotConstant;

import java.util.concurrent.TimeUnit;

@Action(category = VolumeSnapshotConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/volume-snapshots/group/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIRevertVmFromSnapshotGroupEvent.class
)

@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 24)
public class APIRevertVmFromSnapshotGroupMsg extends APIMessage implements VolumeSnapshotGroupMessage {
    @APIParam(resourceType = VolumeSnapshotGroupVO.class)
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
