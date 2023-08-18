package org.zstack.expon.sdk.volume;

import org.springframework.http.HttpMethod;
import org.zstack.expon.sdk.ExponRequest;
import org.zstack.expon.sdk.ExponRestRequest;
import org.zstack.expon.sdk.Param;

import java.util.HashMap;
import java.util.Map;

@ExponRestRequest(
        path = "/block/volumes/{volId}/qos",
        method = HttpMethod.PUT,
        responseClass = SetVolumeQosResponse.class,
        sync = true
)
public class SetVolumeQosRequest extends ExponRequest {
    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Param
    private String volId;
    @Param
    private VolumeQos qos;


    @Override
    protected Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }

    public String getVolId() {
        return volId;
    }

    public void setVolId(String volId) {
        this.volId = volId;
    }

    public VolumeQos getQos() {
        return qos;
    }

    public void setQos(VolumeQos qos) {
        this.qos = qos;
    }
}
