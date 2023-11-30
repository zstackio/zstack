package org.zstack.expon.sdk.volume;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponRequest;
import org.zstack.expon.sdk.ExponRestRequest;
import org.zstack.expon.sdk.Param;

import java.util.HashMap;
import java.util.Map;

@ExponRestRequest(
        path = "/block/snaps/{snapshotId}",
        method = HttpMethod.DELETE,
        responseClass = DeleteVolumeSnapshotResponse.class,
        sync = false
)
public class DeleteVolumeSnapshotRequest extends ExponRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();
    @Param
    private String snapshotId;

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }
}
