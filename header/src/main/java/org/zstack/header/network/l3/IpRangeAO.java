package org.zstack.header.network.l3;

import org.apache.commons.net.util.SubnetUtils;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.Index;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PreUpdate;
import java.sql.Timestamp;

@MappedSuperclass
public class IpRangeAO {
    @Id
    @Column
    private String uuid;
    
    @Column
    @ForeignKey(parentEntityClass = L3NetworkEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String l3NetworkUuid;
    
    @Column
    @Index
    private String name;
    
    @Column
    private String description;

    @Column
    @Index
    private String startIp;
    
    @Column
    @Index
    private String endIp;
    
    @Column
    @Index
    private String netmask;
    
    @Column
    @Index
    private String gateway;

    @Column
    private String networkCidr;

    @Column
    private Timestamp createDate;
    
    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getNetworkCidr() {
        if (networkCidr != null) {
            return networkCidr;
        }

        SubnetUtils su = new SubnetUtils(gateway, netmask);
        return su.getInfo().getCidrSignature();
    }

    public void setNetworkCidr(String networkCidr) {
        this.networkCidr = networkCidr;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStartIp() {
        return startIp;
    }

    public void setStartIp(String startIp) {
        this.startIp = startIp;
    }

    public String getEndIp() {
        return endIp;
    }

    public void setEndIp(String endIp) {
        this.endIp = endIp;
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

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
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
}
