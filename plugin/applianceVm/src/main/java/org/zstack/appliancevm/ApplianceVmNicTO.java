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
    private String category;
    private String l2type;
    private Integer vni;
    private String physicalInterface;

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getL2type() {
        return l2type;
    }

    public void setL2type(String l2type) {
        this.l2type = l2type;
    }

    public Integer getVni() {
        return vni;
    }

    public void setVni(Integer vni) {
        this.vni = vni;
    }

    public String getPhysicalInterface() {
        return physicalInterface;
    }

    public void setPhysicalInterface(String physicalInterface) {
        this.physicalInterface = physicalInterface;
    }
}
