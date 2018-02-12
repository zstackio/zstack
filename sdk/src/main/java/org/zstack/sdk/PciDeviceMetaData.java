package org.zstack.sdk;

public class PciDeviceMetaData  {

    public java.lang.String metaData;
    public void setMetaData(java.lang.String metaData) {
        this.metaData = metaData;
    }
    public java.lang.String getMetaData() {
        return this.metaData;
    }

    public java.util.List<PciDeviceMetaDataEntry> metaDataEntries;
    public void setMetaDataEntries(java.util.List<PciDeviceMetaDataEntry> metaDataEntries) {
        this.metaDataEntries = metaDataEntries;
    }
    public java.util.List<PciDeviceMetaDataEntry> getMetaDataEntries() {
        return this.metaDataEntries;
    }

}
