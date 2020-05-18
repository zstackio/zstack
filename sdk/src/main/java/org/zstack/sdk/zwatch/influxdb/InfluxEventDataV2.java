package org.zstack.sdk.zwatch.influxdb;



public class InfluxEventDataV2 extends org.zstack.sdk.zwatch.influxdb.InfluxEventData {

    public java.lang.String TAG_NAME;
    public void setTAG_NAME(java.lang.String TAG_NAME) {
        this.TAG_NAME = TAG_NAME;
    }
    public java.lang.String getTAG_NAME() {
        return this.TAG_NAME;
    }

    public java.lang.String TAG_ACCOUNT_UUID;
    public void setTAG_ACCOUNT_UUID(java.lang.String TAG_ACCOUNT_UUID) {
        this.TAG_ACCOUNT_UUID = TAG_ACCOUNT_UUID;
    }
    public java.lang.String getTAG_ACCOUNT_UUID() {
        return this.TAG_ACCOUNT_UUID;
    }

    public java.lang.String TAG_TIME;
    public void setTAG_TIME(java.lang.String TAG_TIME) {
        this.TAG_TIME = TAG_TIME;
    }
    public java.lang.String getTAG_TIME() {
        return this.TAG_TIME;
    }

    public java.lang.String TAG_SUBSCRIPTION_UUID;
    public void setTAG_SUBSCRIPTION_UUID(java.lang.String TAG_SUBSCRIPTION_UUID) {
        this.TAG_SUBSCRIPTION_UUID = TAG_SUBSCRIPTION_UUID;
    }
    public java.lang.String getTAG_SUBSCRIPTION_UUID() {
        return this.TAG_SUBSCRIPTION_UUID;
    }

    public java.lang.String TAG_EMERGENCY_LEVEL;
    public void setTAG_EMERGENCY_LEVEL(java.lang.String TAG_EMERGENCY_LEVEL) {
        this.TAG_EMERGENCY_LEVEL = TAG_EMERGENCY_LEVEL;
    }
    public java.lang.String getTAG_EMERGENCY_LEVEL() {
        return this.TAG_EMERGENCY_LEVEL;
    }

    public java.lang.String FIELD_RESOURCE_ID;
    public void setFIELD_RESOURCE_ID(java.lang.String FIELD_RESOURCE_ID) {
        this.FIELD_RESOURCE_ID = FIELD_RESOURCE_ID;
    }
    public java.lang.String getFIELD_RESOURCE_ID() {
        return this.FIELD_RESOURCE_ID;
    }

    public java.lang.String FIELD_DATA_UUID;
    public void setFIELD_DATA_UUID(java.lang.String FIELD_DATA_UUID) {
        this.FIELD_DATA_UUID = FIELD_DATA_UUID;
    }
    public java.lang.String getFIELD_DATA_UUID() {
        return this.FIELD_DATA_UUID;
    }

    public java.lang.String FIELD_ERROR;
    public void setFIELD_ERROR(java.lang.String FIELD_ERROR) {
        this.FIELD_ERROR = FIELD_ERROR;
    }
    public java.lang.String getFIELD_ERROR() {
        return this.FIELD_ERROR;
    }

    public java.lang.String FIELD_RESOURCE_NAME;
    public void setFIELD_RESOURCE_NAME(java.lang.String FIELD_RESOURCE_NAME) {
        this.FIELD_RESOURCE_NAME = FIELD_RESOURCE_NAME;
    }
    public java.lang.String getFIELD_RESOURCE_NAME() {
        return this.FIELD_RESOURCE_NAME;
    }

    public java.lang.String FIELD_NAMESPACE;
    public void setFIELD_NAMESPACE(java.lang.String FIELD_NAMESPACE) {
        this.FIELD_NAMESPACE = FIELD_NAMESPACE;
    }
    public java.lang.String getFIELD_NAMESPACE() {
        return this.FIELD_NAMESPACE;
    }

    public java.lang.String FIELD_READ_STATUS;
    public void setFIELD_READ_STATUS(java.lang.String FIELD_READ_STATUS) {
        this.FIELD_READ_STATUS = FIELD_READ_STATUS;
    }
    public java.lang.String getFIELD_READ_STATUS() {
        return this.FIELD_READ_STATUS;
    }

    public java.util.Set ALL_TAG_NAMES;
    public void setALL_TAG_NAMES(java.util.Set ALL_TAG_NAMES) {
        this.ALL_TAG_NAMES = ALL_TAG_NAMES;
    }
    public java.util.Set getALL_TAG_NAMES() {
        return this.ALL_TAG_NAMES;
    }

    public java.util.Set ALL_FIELD_NAMES;
    public void setALL_FIELD_NAMES(java.util.Set ALL_FIELD_NAMES) {
        this.ALL_FIELD_NAMES = ALL_FIELD_NAMES;
    }
    public java.util.Set getALL_FIELD_NAMES() {
        return this.ALL_FIELD_NAMES;
    }

    public java.util.Set tagsFromV1;
    public void setTagsFromV1(java.util.Set tagsFromV1) {
        this.tagsFromV1 = tagsFromV1;
    }
    public java.util.Set getTagsFromV1() {
        return this.tagsFromV1;
    }

}
