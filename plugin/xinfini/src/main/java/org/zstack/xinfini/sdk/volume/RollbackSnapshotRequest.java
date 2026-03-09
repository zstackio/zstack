package org.zstack.xinfini.sdk.volume;

import org.springframework.http.HttpMethod;
import org.zstack.externalStorage.sdk.Param;
import org.zstack.header.xinfini.XInfiniConstants;
import org.zstack.xinfini.XInfiniApiCategory;
import org.zstack.xinfini.sdk.XInfiniRequest;
import org.zstack.xinfini.sdk.XInfiniRestRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * @ Author : yh.w
 * @ Date   : Created in 17:36 2024/5/27
 */
@XInfiniRestRequest(
    path = "/bs-volumes/{id}/:rollback",
    method = "POST",
    responseClass = RollbackSnapshotResponse.class,
    category = XInfiniApiCategory.AFA
)
public class RollbackSnapshotRequest extends XInfiniRequest {
    @Param
    private int id;

    @Param
    private int bsSnapId;

    @Param
    private String creator = XInfiniConstants.DEFAULT_CREATOR;

    public int getBsSnapId() {
        return bsSnapId;
    }

    public void setBsSnapId(int bsSnapId) {
        this.bsSnapId = bsSnapId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    private static final HashMap<String, Parameter> parameterMap = new HashMap<>();

    @Override
    public Map<String, Parameter> getParameterMap() {
        return parameterMap;
    }

}
