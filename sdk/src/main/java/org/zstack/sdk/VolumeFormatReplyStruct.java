package org.zstack.sdk;

public class VolumeFormatReplyStruct  {

    public java.lang.String format;
    public void setFormat(java.lang.String format) {
        this.format = format;
    }
    public java.lang.String getFormat() {
        return this.format;
    }

    public java.lang.String masterHypervisorType;
    public void setMasterHypervisorType(java.lang.String masterHypervisorType) {
        this.masterHypervisorType = masterHypervisorType;
    }
    public java.lang.String getMasterHypervisorType() {
        return this.masterHypervisorType;
    }

    public java.util.List<String> supportingHypervisorTypes;
    public void setSupportingHypervisorTypes(java.util.List<String> supportingHypervisorTypes) {
        this.supportingHypervisorTypes = supportingHypervisorTypes;
    }
    public java.util.List<String> getSupportingHypervisorTypes() {
        return this.supportingHypervisorTypes;
    }

}
