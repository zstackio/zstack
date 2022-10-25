package org.zstack.sugonSdnController.controller.neutronClient;


import com.google.gson.annotations.SerializedName;

public class TfPortIpEntity {
    @SerializedName("subnet_id")
    private String subnetId;

    @SerializedName("ip_address")
    private String ipAddress;

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
