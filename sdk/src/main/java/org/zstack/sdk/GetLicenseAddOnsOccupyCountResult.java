package org.zstack.sdk;



public class GetLicenseAddOnsOccupyCountResult {
    public int occupyCpuCount;
    public void setOccupyCpuCount(int occupyCpuCount) {
        this.occupyCpuCount = occupyCpuCount;
    }
    public int getOccupyCpuCount() {
        return this.occupyCpuCount;
    }

    public int occupyHostCount;
    public void setOccupyHostCount(int occupyHostCount) {
        this.occupyHostCount = occupyHostCount;
    }
    public int getOccupyHostCount() {
        return this.occupyHostCount;
    }

    public int occupyVmCount;
    public void setOccupyVmCount(int occupyVmCount) {
        this.occupyVmCount = occupyVmCount;
    }
    public int getOccupyVmCount() {
        return this.occupyVmCount;
    }

}
