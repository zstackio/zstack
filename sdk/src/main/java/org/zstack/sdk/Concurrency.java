package org.zstack.sdk;



public class Concurrency  {

    public java.lang.Integer activeOperations;
    public void setActiveOperations(java.lang.Integer activeOperations) {
        this.activeOperations = activeOperations;
    }
    public java.lang.Integer getActiveOperations() {
        return this.activeOperations;
    }

    public java.lang.Integer isolatedTimeoutOperations;
    public void setIsolatedTimeoutOperations(java.lang.Integer isolatedTimeoutOperations) {
        this.isolatedTimeoutOperations = isolatedTimeoutOperations;
    }
    public java.lang.Integer getIsolatedTimeoutOperations() {
        return this.isolatedTimeoutOperations;
    }

}
