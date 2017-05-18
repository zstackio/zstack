package org.zstack.header.network.service;

import org.zstack.header.network.l3.L3NetworkInventory;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 1:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class DhcpStruct {
    private String ip;
    private String mac;
    private String netmask;
    private String gateway;
    private String hostname;
    private L3NetworkInventory l3Network;
    private boolean isDefaultL3Network;
    private String dnsDomain;
    private Integer mtu;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        sb.append(String.format("ip: %s,", ip));
        sb.append(String.format("mac: %s,", mac));
        sb.append(String.format("netmask: %s,", netmask));
        sb.append(String.format("gateway: %s,", gateway));
        sb.append(String.format("hostname: %s,", hostname));
        sb.append(String.format("l3NetworkUuid: %s,", l3Network.getUuid()));
        sb.append(String.format("dnsDomain: %s", dnsDomain));
        sb.append(String.format("isDefaultL3Network: %s", isDefaultL3Network));
        sb.append(String.format("mtu: %s", mtu));
        sb.append("]");
        return sb.toString();
    }

    public String getDnsDomain() {
        return dnsDomain;
    }

    public void setDnsDomain(String domain) {
        this.dnsDomain = domain;
    }

    public boolean isDefaultL3Network() {
        return isDefaultL3Network;
    }

    public void setDefaultL3Network(boolean isDefaultL3Network) {
        this.isDefaultL3Network = isDefaultL3Network;
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

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public L3NetworkInventory getL3Network() {
        return l3Network;
    }

    public void setL3Network(L3NetworkInventory l3Network) {
        this.l3Network = l3Network;
    }

    public Integer getMtu() {
        return mtu;
    }

    public void setMtu(Integer mtu) {
        this.mtu = mtu;
    }

}
