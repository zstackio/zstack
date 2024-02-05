package org.zstack.header.storage.snapshot.group;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.snapshot.SnapshotBackendOperation;
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
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 36)
public class APIDeleteVolumeSnapshotGroupMsg extends APIDeleteMessage implements VolumeSnapshotGroupMessage {
    @APIParam(resourceType = VolumeSnapshotGroupVO.class, successIfResourceNotExisting = true)
    private String uuid;

    @APIParam(required = false)
    private boolean onlySelf = false;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isOnlySelf() {
        return onlySelf;
    }

    public void setOnlySelf(boolean onlySelf) {
        this.onlySelf = onlySelf;
    }

    @Override
    public String getGroupUuid() {
        return uuid;
    }

    @Override
    public SnapshotBackendOperation getBackendOperation() {
        return SnapshotBackendOperation.FILE_DELETION;
    }

    public static APIDeleteVolumeSnapshotGroupMsg __example__() {
        APIDeleteVolumeSnapshotGroupMsg result = new APIDeleteVolumeSnapshotGroupMsg();
        result.uuid = uuid();
        return result;
    }
}
