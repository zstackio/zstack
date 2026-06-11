package org.zstack.rest;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeDiagnostic;
import org.zstack.header.errorcode.ErrorCodeDiagnosticHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by xing5 on 2016/12/8.
 */
public class ApiResponse extends HashMap {
    private String id;
    private String success;
    private String location;
    private ErrorCode error;
    private ErrorCodeDiagnostic diagnostic;
    private Map<String, String> schema;
    private long apiTimeout;

    public String getId() {
        return id;
    }

    public String getSuccess() {
        return success;
    }

    public long getApiTimeout() {
        return apiTimeout;
    }
 
    public String getLocation() {
        return location;
    }

    public Map<String, String> getSchema() {
        return schema;
    }

    public void setApiTimeout(long apiTimeout) {
        this.apiTimeout = apiTimeout;
        put("apiTimeout", apiTimeout);
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

    public ErrorCodeDiagnostic getDiagnostic() {
        return diagnostic;
    }

    public void setId(String id) {
        this.id = id;
        put("id", id);
    }

    public void setSuccess(boolean success) {
        this.success = String.valueOf(success);
        put("success", this.success);
    }

    public void setDiagnostic(ErrorCodeDiagnostic diagnostic) {
        this.diagnostic = diagnostic;
        put("diagnostic", diagnostic);
    }

    public void setError(ErrorCode error) {
        this.error = error;
        put("error", error);
    }

    public void completeFailure(String id, String locale) {
        setId(id == null ? UUID.randomUUID().toString().replace("-", "") : id);
        setSuccess(false);
        setDiagnostic(ErrorCodeDiagnosticHelper.toDiagnostic(error, locale));
    }
}
