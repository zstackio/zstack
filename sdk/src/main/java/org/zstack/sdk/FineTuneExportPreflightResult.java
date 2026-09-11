package org.zstack.sdk;



public class FineTuneExportPreflightResult  {

    public boolean passed;
    public void setPassed(boolean passed) {
        this.passed = passed;
    }
    public boolean getPassed() {
        return this.passed;
    }

    public java.util.List checks;
    public void setChecks(java.util.List checks) {
        this.checks = checks;
    }
    public java.util.List getChecks() {
        return this.checks;
    }

    public java.lang.String installPath;
    public void setInstallPath(java.lang.String installPath) {
        this.installPath = installPath;
    }
    public java.lang.String getInstallPath() {
        return this.installPath;
    }

    public java.lang.String framework;
    public void setFramework(java.lang.String framework) {
        this.framework = framework;
    }
    public java.lang.String getFramework() {
        return this.framework;
    }

    public java.lang.String architecture;
    public void setArchitecture(java.lang.String architecture) {
        this.architecture = architecture;
    }
    public java.lang.String getArchitecture() {
        return this.architecture;
    }

    public java.util.List fileTypes;
    public void setFileTypes(java.util.List fileTypes) {
        this.fileTypes = fileTypes;
    }
    public java.util.List getFileTypes() {
        return this.fileTypes;
    }

    public java.lang.Long sizeBytes;
    public void setSizeBytes(java.lang.Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }
    public java.lang.Long getSizeBytes() {
        return this.sizeBytes;
    }

    public java.lang.String fingerprint;
    public void setFingerprint(java.lang.String fingerprint) {
        this.fingerprint = fingerprint;
    }
    public java.lang.String getFingerprint() {
        return this.fingerprint;
    }

    public java.lang.String exportId;
    public void setExportId(java.lang.String exportId) {
        this.exportId = exportId;
    }
    public java.lang.String getExportId() {
        return this.exportId;
    }

}
