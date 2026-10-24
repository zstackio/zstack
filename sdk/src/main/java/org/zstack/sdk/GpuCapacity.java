package org.zstack.sdk;



public class GpuCapacity  {

    public boolean passed;
    public void setPassed(boolean passed) {
        this.passed = passed;
    }
    public boolean getPassed() {
        return this.passed;
    }

    public int requiredHostCount;
    public void setRequiredHostCount(int requiredHostCount) {
        this.requiredHostCount = requiredHostCount;
    }
    public int getRequiredHostCount() {
        return this.requiredHostCount;
    }

    public int qualifiedHostCount;
    public void setQualifiedHostCount(int qualifiedHostCount) {
        this.qualifiedHostCount = qualifiedHostCount;
    }
    public int getQualifiedHostCount() {
        return this.qualifiedHostCount;
    }

    public java.lang.String gpuSpecUuid;
    public void setGpuSpecUuid(java.lang.String gpuSpecUuid) {
        this.gpuSpecUuid = gpuSpecUuid;
    }
    public java.lang.String getGpuSpecUuid() {
        return this.gpuSpecUuid;
    }

    public java.lang.String reason;
    public void setReason(java.lang.String reason) {
        this.reason = reason;
    }
    public java.lang.String getReason() {
        return this.reason;
    }

    public java.lang.String errorCode;
    public void setErrorCode(java.lang.String errorCode) {
        this.errorCode = errorCode;
    }
    public java.lang.String getErrorCode() {
        return this.errorCode;
    }

}
