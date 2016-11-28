package org.zstack.header.network.l3;

import org.zstack.header.rest.APINoSee;

import java.io.Serializable;
import java.sql.Timestamp;

public class UsedIpInventory implements Serializable {
    private String uuid;
    private String ipRangeUuid;
    private String l3NetworkUuid;
    private String ip;
    private String netmask;
    private String gateway;
    private String usedFor;
    @APINoSee
    private String metaData;
    private long ipInLong;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static UsedIpInventory valueOf(UsedIpVO vo) {
        UsedIpInventory inv = new UsedIpInventory();
        inv.setCreateDate(vo.getCreateDate());
        inv.setUuid(vo.getUuid());
        inv.setIp(vo.getIp());
        inv.setIpInLong(vo.getIpInLong());
        inv.setIpRangeUuid(vo.getIpRangeUuid());
        inv.setL3NetworkUuid(vo.getL3NetworkUuid());
        inv.setGateway(vo.getGateway());
        inv.setNetmask(vo.getNetmask());
        inv.setUsedFor(vo.getUsedFor());
        inv.setMetaData(vo.getMetaData());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getIpRangeUuid() {
        return ipRangeUuid;
    }

    public void setIpRangeUuid(String ipRangeUuid) {
        this.ipRangeUuid = ipRangeUuid;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public long getIpInLong() {
        return ipInLong;
    }

    public void setIpInLong(long ipInLong) {
        this.ipInLong = ipInLong;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
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

    public String getUsedFor() {
        return usedFor;
    }

    public void setUsedFor(String usedFor) {
        this.usedFor = usedFor;
    }

    public String getMetaData() {
        return metaData;
    }

    public void setMetaData(String metaData) {
        this.metaData = metaData;
    }
}
