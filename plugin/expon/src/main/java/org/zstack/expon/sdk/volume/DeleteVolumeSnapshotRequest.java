package org.zstack.expon.sdk.volume;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponRequest;
import org.zstack.expon.sdk.ExponRestRequest;

import java.util.HashMap;
import java.util.Map;

@ExponRestRequest(
        path = "/block/snaps/{snapshotId}",
        method = HttpMethod.DELETE,
        responseClass = DeleteVolumeSnapshotResponse.class,
        sync = true
)
public class DeleteVolumeSnapshotRequest extends ExponRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();
    private String snapshotId;

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    @Override
    protected Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
}
