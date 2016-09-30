package org.zstack.appliancevm;

import org.zstack.header.vm.VmNicInventory;

/**
 */
public class ApplianceVmNicTO {
    private String ip;
    private String netmask;
    private String gateway;
    private String mac;
    private String deviceName;
    private String metaData;
    private boolean isDefaultRoute;

    public ApplianceVmNicTO(VmNicInventory inv) {
        ip = inv.getIp();
        netmask = inv.getNetmask();
        gateway = inv.getGateway();
        mac = inv.getMac();
    }

    public ApplianceVmNicTO() {
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
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

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getMetaData() {
        return metaData;
    }

    public void setMetaData(String metaData) {
        this.metaData = metaData;
    }

    public boolean isDefaultRoute() {
        return isDefaultRoute;
    }

    public void setDefaultRoute(boolean isDefaultRoute) {
        this.isDefaultRoute = isDefaultRoute;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }
}
