package org.zstack.expon.sdk.volume;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponRequest;
import org.zstack.expon.sdk.ExponRestRequest;
import org.zstack.expon.sdk.Param;

import java.util.HashMap;
import java.util.Map;

@ExponRestRequest(
        path = "/block/snaps/{snapshotId}/clone",
        method = HttpMethod.POST,
        responseClass = CloneVolumeResponse.class,
        // TODO: change to true
        sync = false
)
public class CloneVolumeRequest extends ExponRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Param
    private String snapshotId;

    @Param
    private String name;

    @Param
    private VolumeQos qos = new VolumeQos();


    @Override
    protected Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VolumeQos getQos() {
        return qos;
    }

    public void setQos(VolumeQos qos) {
        this.qos = qos;
    }
}
