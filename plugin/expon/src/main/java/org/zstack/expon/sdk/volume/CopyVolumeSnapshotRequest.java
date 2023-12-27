package org.zstack.expon.sdk.volume;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponRequest;
import org.zstack.expon.sdk.ExponRestRequest;
import org.zstack.expon.sdk.Param;

import java.util.HashMap;
import java.util.Map;

@ExponRestRequest(
        path = "/block/snaps/{snapshotId}/copy_clone",
        method = HttpMethod.POST,
        responseClass = CloneVolumeResponse.class,
        sync = true
)
public class CopyVolumeSnapshotRequest extends ExponRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Param
    private String snapshotId;

    @Param
    private String name;

    @Param
    private String phyPoolId;

    @Param
    private ExponVolumeQos qos = new ExponVolumeQos();


    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public String getPhyPoolId() {
        return phyPoolId;
    }

    public void setPhyPoolId(String phyPoolId) {
        this.phyPoolId = phyPoolId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ExponVolumeQos getQos() {
        return qos;
    }

    public void setQos(ExponVolumeQos qos) {
        this.qos = qos;
    }
}
