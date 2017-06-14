package org.zstack.header.vm;

import org.zstack.header.network.l3.L3NetworkEO;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.search.SqlTrigger;
import org.zstack.header.search.TriggerIndex;
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
public class VmNicVO extends ResourceVO {
    @Column
    @ForeignKey(parentEntityClass = VmInstanceEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String vmInstanceUuid;

    @Column
    @ForeignKey(parentEntityClass = UsedIpVO.class, onDeleteAction = ReferenceOption.SET_NULL)
    private String usedIpUuid;

    @Column
    @ForeignKey(parentEntityClass = L3NetworkEO.class, onDeleteAction = ReferenceOption.SET_NULL)
    private String l3NetworkUuid;

    @Column
    @Index
    private String ip;

    @Column
    private String netmask;

    @Column
    private String gateway;

    @Column
    @Index
    private String mac;

    @Column
    private String metaData;

    @Column
    private int deviceId;

    @Column
    private String internalName;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getUsedIpUuid() {
        return usedIpUuid;
    }

    public void setUsedIpUuid(String usedIpUuid) {
        this.usedIpUuid = usedIpUuid;
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

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
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

    public String getInternalName() {
        return internalName;
    }

    public void setInternalName(String internalName) {
        this.internalName = internalName;
    }

    public static String generateNicInternalName(long vmInternalId, long nicDeviceId) {
        return String.format("vnic%s.%s", vmInternalId, nicDeviceId);
    }
}
