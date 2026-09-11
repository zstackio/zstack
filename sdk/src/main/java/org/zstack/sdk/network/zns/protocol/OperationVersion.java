package org.zstack.sdk.network.zns.protocol;



public class OperationVersion  {

    public int version;
    public void setVersion(int version) {
        this.version = version;
    }
    public int getVersion() {
        return this.version;
    }

    public boolean supported;
    public void setSupported(boolean supported) {
        this.supported = supported;
    }
    public boolean getSupported() {
        return this.supported;
    }

    public java.lang.String method;
    public void setMethod(java.lang.String method) {
        this.method = method;
    }
    public java.lang.String getMethod() {
        return this.method;
    }

    public java.lang.String path;
    public void setPath(java.lang.String path) {
        this.path = path;
    }
    public java.lang.String getPath() {
        return this.path;
    }

    public java.lang.String mode;
    public void setMode(java.lang.String mode) {
        this.mode = mode;
    }
    public java.lang.String getMode() {
        return this.mode;
    }

    public java.util.List fields;
    public void setFields(java.util.List fields) {
        this.fields = fields;
    }
    public java.util.List getFields() {
        return this.fields;
    }

    public java.util.List errors;
    public void setErrors(java.util.List errors) {
        this.errors = errors;
    }
    public java.util.List getErrors() {
        return this.errors;
    }

    public java.lang.String reason;
    public void setReason(java.lang.String reason) {
        this.reason = reason;
    }
    public java.lang.String getReason() {
        return this.reason;
    }

}
