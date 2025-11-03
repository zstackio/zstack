package org.zstack.sdk;

import org.zstack.sdk.ModelServiceMatchStatus;
import org.zstack.sdk.ModelServiceMatchEntryName;

public class ModelServiceMatchEntry  {

    public ModelServiceMatchStatus status;
    public void setStatus(ModelServiceMatchStatus status) {
        this.status = status;
    }
    public ModelServiceMatchStatus getStatus() {
        return this.status;
    }

    public ModelServiceMatchEntryName name;
    public void setName(ModelServiceMatchEntryName name) {
        this.name = name;
    }
    public ModelServiceMatchEntryName getName() {
        return this.name;
    }

    public java.lang.String fieldName;
    public void setFieldName(java.lang.String fieldName) {
        this.fieldName = fieldName;
    }
    public java.lang.String getFieldName() {
        return this.fieldName;
    }

    public java.lang.String fieldValue;
    public void setFieldValue(java.lang.String fieldValue) {
        this.fieldValue = fieldValue;
    }
    public java.lang.String getFieldValue() {
        return this.fieldValue;
    }

    public java.lang.String comments;
    public void setComments(java.lang.String comments) {
        this.comments = comments;
    }
    public java.lang.String getComments() {
        return this.comments;
    }

}
