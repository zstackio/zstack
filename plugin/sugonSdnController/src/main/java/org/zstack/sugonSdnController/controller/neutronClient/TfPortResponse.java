package org.zstack.sugonSdnController.controller.neutronClient;


import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TfPortResponse {
    private String msg;
    private int code;
    private String macAddress;
    private List<TfPortIpEntity> fixedIps;
    private String exception;

    @SerializedName("id")
    private String portId;

    @Override
    public String toString() {
        return "TfPortResponse{" +
                "msg='" + msg + '\'' +
                ", code=" + code +
                ", macAddress='" + macAddress + '\'' +
                ", fixedIps=" + fixedIps +
                ", exception='" + exception + '\'' +
                ", portId='" + portId + '\'' +
                '}';
    }

    public String getPortId() {
        return portId;
    }

    public void setPortId(String portId) {
        this.portId = portId;
    }

    public TfPortResponse(String msg){
        this.msg = msg;
    }

    public TfPortResponse(){}

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public List<TfPortIpEntity> getFixedIps() {
        return fixedIps;
    }

    public void setFixedIps(List<TfPortIpEntity> fixedIps) {
        this.fixedIps = fixedIps;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }
}
