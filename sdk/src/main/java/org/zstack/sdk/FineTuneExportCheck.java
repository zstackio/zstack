package org.zstack.sdk;



public class FineTuneExportCheck  {

    public java.lang.String code;
    public void setCode(java.lang.String code) {
        this.code = code;
    }
    public java.lang.String getCode() {
        return this.code;
    }

    public boolean passed;
    public void setPassed(boolean passed) {
        this.passed = passed;
    }
    public boolean getPassed() {
        return this.passed;
    }

    public java.lang.String message;
    public void setMessage(java.lang.String message) {
        this.message = message;
    }
    public java.lang.String getMessage() {
        return this.message;
    }

    public java.util.List missingKeys;
    public void setMissingKeys(java.util.List missingKeys) {
        this.missingKeys = missingKeys;
    }
    public java.util.List getMissingKeys() {
        return this.missingKeys;
    }

    public java.lang.Integer missingKeyCount;
    public void setMissingKeyCount(java.lang.Integer missingKeyCount) {
        this.missingKeyCount = missingKeyCount;
    }
    public java.lang.Integer getMissingKeyCount() {
        return this.missingKeyCount;
    }

}
