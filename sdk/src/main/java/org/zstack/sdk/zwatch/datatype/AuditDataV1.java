package org.zstack.sdk.zwatch.datatype;



public class AuditDataV1 extends org.zstack.sdk.zwatch.datatype.AuditData {

    public java.lang.String TAG_RESOURCE_UUID;
    public void setTAG_RESOURCE_UUID(java.lang.String TAG_RESOURCE_UUID) {
        this.TAG_RESOURCE_UUID = TAG_RESOURCE_UUID;
    }
    public java.lang.String getTAG_RESOURCE_UUID() {
        return this.TAG_RESOURCE_UUID;
    }

    public java.lang.String TAG_RESOURCE_TYPE;
    public void setTAG_RESOURCE_TYPE(java.lang.String TAG_RESOURCE_TYPE) {
        this.TAG_RESOURCE_TYPE = TAG_RESOURCE_TYPE;
    }
    public java.lang.String getTAG_RESOURCE_TYPE() {
        return this.TAG_RESOURCE_TYPE;
    }

    public java.lang.String TAG_API_NAME;
    public void setTAG_API_NAME(java.lang.String TAG_API_NAME) {
        this.TAG_API_NAME = TAG_API_NAME;
    }
    public java.lang.String getTAG_API_NAME() {
        return this.TAG_API_NAME;
    }

    public java.lang.String TAG_API_ERROR;
    public void setTAG_API_ERROR(java.lang.String TAG_API_ERROR) {
        this.TAG_API_ERROR = TAG_API_ERROR;
    }
    public java.lang.String getTAG_API_ERROR() {
        return this.TAG_API_ERROR;
    }

    public java.lang.String TAG_API_OPERATOR_ACCOUNT_UUID;
    public void setTAG_API_OPERATOR_ACCOUNT_UUID(java.lang.String TAG_API_OPERATOR_ACCOUNT_UUID) {
        this.TAG_API_OPERATOR_ACCOUNT_UUID = TAG_API_OPERATOR_ACCOUNT_UUID;
    }
    public java.lang.String getTAG_API_OPERATOR_ACCOUNT_UUID() {
        return this.TAG_API_OPERATOR_ACCOUNT_UUID;
    }

    public java.lang.String TAG_API_CLIENTIP;
    public void setTAG_API_CLIENTIP(java.lang.String TAG_API_CLIENTIP) {
        this.TAG_API_CLIENTIP = TAG_API_CLIENTIP;
    }
    public java.lang.String getTAG_API_CLIENTIP() {
        return this.TAG_API_CLIENTIP;
    }

    public java.lang.String TAG_API_CLIENTBROWSER;
    public void setTAG_API_CLIENTBROWSER(java.lang.String TAG_API_CLIENTBROWSER) {
        this.TAG_API_CLIENTBROWSER = TAG_API_CLIENTBROWSER;
    }
    public java.lang.String getTAG_API_CLIENTBROWSER() {
        return this.TAG_API_CLIENTBROWSER;
    }

    public java.util.Set queryableLabels;
    public void setQueryableLabels(java.util.Set queryableLabels) {
        this.queryableLabels = queryableLabels;
    }
    public java.util.Set getQueryableLabels() {
        return this.queryableLabels;
    }

    public java.util.Set queryableLoginLabels;
    public void setQueryableLoginLabels(java.util.Set queryableLoginLabels) {
        this.queryableLoginLabels = queryableLoginLabels;
    }
    public java.util.Set getQueryableLoginLabels() {
        return this.queryableLoginLabels;
    }

}
