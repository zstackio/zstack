package org.zstack.xinfini.sdk.vhost;

import org.springframework.http.HttpMethod;
import org.zstack.externalStorage.sdk.Param;
import org.zstack.xinfini.XInfiniApiCategory;
import org.zstack.xinfini.sdk.XInfiniRequest;
import org.zstack.xinfini.sdk.XInfiniRestRequest;

import java.util.HashMap;
import java.util.Map;

@XInfiniRestRequest(path = "/bdcs/{id}", method = HttpMethod.PATCH,
        responseClass = GetBdcResponse.class, category = XInfiniApiCategory.AFA)
public class UpdateBdcRequest extends XInfiniRequest {
    @Param
    private int id;

    @Param
    private String storageFencedState;

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStorageFencedState() {
        return storageFencedState;
    }

    public void setStorageFencedState(String storageFencedState) {
        this.storageFencedState = storageFencedState;
    }
}
