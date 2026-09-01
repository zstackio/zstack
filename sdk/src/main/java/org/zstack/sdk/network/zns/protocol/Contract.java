package org.zstack.sdk.network.zns.protocol;



public class Contract  {

    public int version;
    public void setVersion(int version) {
        this.version = version;
    }
    public int getVersion() {
        return this.version;
    }

    public boolean supported;
    public void setSupported(boolean supported) {
        this.supported = supported;
    }
    public boolean getSupported() {
        return this.supported;
    }

    public boolean initialInstallable;
    public void setInitialInstallable(boolean initialInstallable) {
        this.initialInstallable = initialInstallable;
    }
    public boolean getInitialInstallable() {
        return this.initialInstallable;
    }

    public java.util.List operations;
    public void setOperations(java.util.List operations) {
        this.operations = operations;
    }
    public java.util.List getOperations() {
        return this.operations;
    }

    public java.util.List events;
    public void setEvents(java.util.List events) {
        this.events = events;
    }
    public java.util.List getEvents() {
        return this.events;
    }

    public java.util.List errors;
    public void setErrors(java.util.List errors) {
        this.errors = errors;
    }
    public java.util.List getErrors() {
        return this.errors;
    }

    public java.lang.String reason;
    public void setReason(java.lang.String reason) {
        this.reason = reason;
    }
    public java.lang.String getReason() {
        return this.reason;
    }

}
