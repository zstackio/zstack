package org.zstack.sdk;



public class FineTuneMergeReport  {

    public java.lang.String taskUuid;
    public void setTaskUuid(java.lang.String taskUuid) {
        this.taskUuid = taskUuid;
    }
    public java.lang.String getTaskUuid() {
        return this.taskUuid;
    }

    public java.lang.Boolean tensorKeyMatch;
    public void setTensorKeyMatch(java.lang.Boolean tensorKeyMatch) {
        this.tensorKeyMatch = tensorKeyMatch;
    }
    public java.lang.Boolean getTensorKeyMatch() {
        return this.tensorKeyMatch;
    }

    public java.lang.Integer baseTensorCount;
    public void setBaseTensorCount(java.lang.Integer baseTensorCount) {
        this.baseTensorCount = baseTensorCount;
    }
    public java.lang.Integer getBaseTensorCount() {
        return this.baseTensorCount;
    }

    public java.lang.Integer outputTensorCount;
    public void setOutputTensorCount(java.lang.Integer outputTensorCount) {
        this.outputTensorCount = outputTensorCount;
    }
    public java.lang.Integer getOutputTensorCount() {
        return this.outputTensorCount;
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

    public java.lang.Integer extraKeyCount;
    public void setExtraKeyCount(java.lang.Integer extraKeyCount) {
        this.extraKeyCount = extraKeyCount;
    }
    public java.lang.Integer getExtraKeyCount() {
        return this.extraKeyCount;
    }

    public java.lang.Double elapsedSeconds;
    public void setElapsedSeconds(java.lang.Double elapsedSeconds) {
        this.elapsedSeconds = elapsedSeconds;
    }
    public java.lang.Double getElapsedSeconds() {
        return this.elapsedSeconds;
    }

    public java.lang.String dtype;
    public void setDtype(java.lang.String dtype) {
        this.dtype = dtype;
    }
    public java.lang.String getDtype() {
        return this.dtype;
    }

}
