package org.zstack.header.vm;

import java.io.Serializable;

public class VmNicParm implements Serializable {
    private String l3NetworkUuid;

    private String ip;

    private String mac;

    private String netmask;

    private String gateway;

    private String metaData;

    private Integer ipVersion;

    private String driverType;

    private String state = VmNicState.enable.toString();

    private Long outboundBandwidth;

    private Long inboundBandwidth;

    private Integer multiQueueNum;

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getMetaData() {
        return metaData;
    }

    public void setMetaData(String metaData) {
        this.metaData = metaData;
    }

    public Integer getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
    }

    public String getDriverType() {
        return driverType;
    }

    public void setDriverType(String driverType) {
        this.driverType = driverType;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Long getOutboundBandwidth() {
        return outboundBandwidth;
    }

    public void setOutboundBandwidth(Long outboundBandwidth) {
        this.outboundBandwidth = outboundBandwidth;
    }

    public Long getInboundBandwidth() {
        return inboundBandwidth;
    }

    public void setInboundBandwidth(Long inboundBandwidth) {
        this.inboundBandwidth = inboundBandwidth;
    }

    public Integer getMultiQueueNum() {
        return multiQueueNum;
    }

    public void setMultiQueueNum(Integer multiQueueNum) {
        this.multiQueueNum = multiQueueNum;
    }
}
