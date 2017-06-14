package org.zstack.network.service.vip;

import org.zstack.header.network.l3.IpRangeEO;
import org.zstack.header.network.l3.L3NetworkEO;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.Index;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@BaseResource
public class VipVO extends ResourceVO {
    @Column
    @Index
    private String  name;

    @Column
    private String  description;

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
    @Enumerated(EnumType.STRING)
    private VipState state;

    @Column
    private String netmask;

    @Column
    private String serviceProvider;

    @Column
    private String useFor;

    @Column
    private String usedIpUuid;

    @Column
    @ForeignKey(parentEntityClass = L3NetworkEO.class, onDeleteAction = ReferenceOption.SET_NULL)
    private String peerL3NetworkUuid;

    @Column
    private Timestamp createDate;
    
    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getUsedIpUuid() {
        return usedIpUuid;
    }

    public void setUsedIpUuid(String usedIpUuid) {
        this.usedIpUuid = usedIpUuid;
    }

    public VipState getState() {
        return state;
    }

    public void setState(VipState state) {
        this.state = state;
    }

    public String getPeerL3NetworkUuid() {
        return peerL3NetworkUuid;
    }

    public void setPeerL3NetworkUuid(String peerL3NetworkUuid) {
        this.peerL3NetworkUuid = peerL3NetworkUuid;
    }

    public String getServiceProvider() {
        return serviceProvider;
    }

    public void setServiceProvider(String serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    public String getUseFor() {
        return useFor;
    }

    public void setUseFor(String useFor) {
        this.useFor = useFor;
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

    public String getIpRangeUuid() {
        return ipRangeUuid;
    }

    public void setIpRangeUuid(String ipRangeUuid) {
        this.ipRangeUuid = ipRangeUuid;
    }

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
}
