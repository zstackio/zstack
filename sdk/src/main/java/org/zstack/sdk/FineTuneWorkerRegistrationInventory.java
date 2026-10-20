package org.zstack.sdk;



public class FineTuneWorkerRegistrationInventory  {

    public boolean reachable;
    public void setReachable(boolean reachable) {
        this.reachable = reachable;
    }
    public boolean getReachable() {
        return this.reachable;
    }

    public boolean registered;
    public void setRegistered(boolean registered) {
        this.registered = registered;
    }
    public boolean getRegistered() {
        return this.registered;
    }

    public int nnodes;
    public void setNnodes(int nnodes) {
        this.nnodes = nnodes;
    }
    public int getNnodes() {
        return this.nnodes;
    }

    public java.util.List registeredRanks;
    public void setRegisteredRanks(java.util.List registeredRanks) {
        this.registeredRanks = registeredRanks;
    }
    public java.util.List getRegisteredRanks() {
        return this.registeredRanks;
    }

    public java.util.List missingRanks;
    public void setMissingRanks(java.util.List missingRanks) {
        this.missingRanks = missingRanks;
    }
    public java.util.List getMissingRanks() {
        return this.missingRanks;
    }

    public java.util.List staleRanks;
    public void setStaleRanks(java.util.List staleRanks) {
        this.staleRanks = staleRanks;
    }
    public java.util.List getStaleRanks() {
        return this.staleRanks;
    }

}
