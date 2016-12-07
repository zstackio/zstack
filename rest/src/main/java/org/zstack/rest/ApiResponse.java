package org.zstack.rest;

import org.zstack.header.errorcode.ErrorCode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by xing5 on 2016/12/8.
 */
public class ApiResponse extends HashMap {
    private String location;
    private ErrorCode error;
    private Object result;
    private Map<String, String> schema;

    public String getLocation() {
        return location;
    }

    public Map<String, String> getSchema() {
        return schema;
    }

    public void setSchema(Map<String, String> schema) {
        this.schema = schema;
        put("schema", schema);
    }

    public void setLocation(String location) {
        this.location = location;
        put("location", location);
    }

    public ErrorCode getError() {
        return error;
    }

    public void setError(ErrorCode error) {
        this.error = error;
        put("error", error);
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
        put("result", result);
    }
}
