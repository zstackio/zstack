package org.zstack.header.network.l3;

import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.Index;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table
public class UsedIpVO {
    @Id
    @Column
    private String uuid;

    @Column
    @ForeignKey(parentEntityClass = IpRangeEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String ipRangeUuid;

    @Column
    @ForeignKey(parentEntityClass = L3NetworkEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String l3NetworkUuid;

    @Column
    @Index
    private String ip;

    @Column
    private String gateway;

    @Column
    private String netmask;

    @Column
    @Index
    private long ipInLong;

    @Column
    private String usedFor;

    @Column
    private String metaData;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public UsedIpVO(String rangeUuid, String ip) {
        this.ip = ip;
        this.ipRangeUuid = rangeUuid;
    }

    private UsedIpVO() {
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

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public String getUsedFor() {
        return usedFor;
    }

    public void setUsedFor(String usedFor) {
        this.usedFor = usedFor;
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

    public String getMetaData() {
        return metaData;
    }

    public void setMetaData(String metaData) {
        this.metaData = metaData;
    }
}
