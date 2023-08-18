package org.zstack.expon.sdk.volume;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponRequest;
import org.zstack.expon.sdk.ExponRestRequest;
import org.zstack.expon.sdk.Param;

import java.util.HashMap;
import java.util.Map;

@ExponRestRequest(
        path = "/block/volumes",
        method = HttpMethod.POST,
        responseClass = CreateVolumeResponse.class,
        sync = true
)
public class CreateVolumeRequest extends ExponRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Param
    private String name;
    @Param
    private String phyPoolId;
    @Param
    private long volumeSize;
    @Param
    private VolumeQos qos = new VolumeQos();

    @Override
    protected Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhyPoolId() {
        return phyPoolId;
    }

    public void setPhyPoolId(String phyPoolId) {
        this.phyPoolId = phyPoolId;
    }

    public long getVolumeSize() {
        return volumeSize;
    }

    public void setVolumeSize(long volumeSize) {
        this.volumeSize = volumeSize;
    }

    public VolumeQos getQos() {
        return qos;
    }

    public void setQos(VolumeQos qos) {
        this.qos = qos;
    }
}
