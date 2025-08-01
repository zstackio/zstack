package org.zstack.sdk;

import org.zstack.sdk.ErrorCode;

public class UpdateLicenseView  {

    public java.lang.String license;
    public void setLicense(java.lang.String license) {
        this.license = license;
    }
    public java.lang.String getLicense() {
        return this.license;
    }

    public ErrorCode error;
    public void setError(ErrorCode error) {
        this.error = error;
    }
    public ErrorCode getError() {
        return this.error;
    }

    public java.lang.String handleBy;
    public void setHandleBy(java.lang.String handleBy) {
        this.handleBy = handleBy;
    }
    public java.lang.String getHandleBy() {
        return this.handleBy;
    }

}
