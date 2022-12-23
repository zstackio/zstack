package org.zstack.sugonSdnController.controller.neutronClient;


import com.google.gson.annotations.SerializedName;

public class TfPortIpEntity {
    @SerializedName("subnet_id")
    private String subnetId;

    @SerializedName("ip_address")
    private String ipAddress;

    private String workType;

    public String getWorkType() {
        return workType;
    }

    public void setWorkType(String workType) {
        this.workType = workType;
    }

    public String getSubnetId() {
        return subnetId;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public String toString() {
        return "IpEntity{" +
                "subnetId='" + subnetId + '\'' +
                ", subnetId='" + ipAddress + '\'' +
                '}';
    }
}
