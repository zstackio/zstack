package org.zstack.header.errorcode;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class ErrorCodeDiagnostic implements Serializable {
    private String component;
    private String category;
    private String code;
    private String message;
    private boolean reviewed;
    @SerializedName("raw_message")
    private RawMessage rawMessage;

    public static class RawMessage implements Serializable {
        private String symptom;
        private String cause;
        private String solution;
        private String note;

        public String getSymptom() {
            return symptom;
        }

        public void setSymptom(String symptom) {
            this.symptom = symptom;
        }

        public String getCause() {
            return cause;
        }

        public void setCause(String cause) {
            this.cause = cause;
        }

        public String getSolution() {
            return solution;
        }

        public void setSolution(String solution) {
            this.solution = solution;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isReviewed() {
        return reviewed;
    }

    public void setReviewed(boolean reviewed) {
        this.reviewed = reviewed;
    }

    public RawMessage getRawMessage() {
        return rawMessage;
    }

    public void setRawMessage(RawMessage rawMessage) {
        this.rawMessage = rawMessage;
    }
}
