package org.zstack.sdk;

import org.zstack.sdk.ErrorCode;
import org.zstack.sdk.VmInstanceInventory;

public class CloneVmInstanceInventory  {

    public ErrorCode error;
    public void setError(ErrorCode error) {
        this.error = error;
    }
    public ErrorCode getError() {
        return this.error;
    }

    public VmInstanceInventory inventory;
    public void setInventory(VmInstanceInventory inventory) {
        this.inventory = inventory;
    }
    public VmInstanceInventory getInventory() {
        return this.inventory;
    }

    public boolean started;
    public void setStarted(boolean started) {
        this.started = started;
    }
    public boolean getStarted() {
        return this.started;
    }

    public java.lang.String volumeQos;
    public void setVolumeQos(java.lang.String volumeQos) {
        this.volumeQos = volumeQos;
    }
    public java.lang.String getVolumeQos() {
        return this.volumeQos;
    }

}
