package org.zstack.physicalNetworkInterface.header;

import org.zstack.header.host.HostEO;
import org.zstack.header.host.NicVirtStatus;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.Index;
import org.zstack.header.vo.*;
import org.zstack.header.volume.VolumeVO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by GuoYi on 4/23/20.
 */
@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = HostEO.class, joinColumn = "hostUuid")
})
public class HostNetworkInterfaceVO extends ResourceVO implements ToInventory, OwnedByAccount {
    @Index
    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostUuid;

    @Index
    @Column
    @ForeignKey(parentEntityClass = HostNetworkBondingVO.class, onDeleteAction = ForeignKey.ReferenceOption.SET_NULL)
    private String bondingUuid;

    @Column
    private String interfaceName;

    @Column
    private String interfaceType;

    @Column
    private Long speed;

    @Column
    private boolean slaveActive;

    @Column
    private boolean carrierActive;

    @Column
    private String ipAddresses;

    @Column
    private Integer maxPartNum;

    @Column
    private String gateway;

    @Column
    private String mac;

    @Column
    private String callBackIp;

    @Column
    private String pciDeviceAddress;

    @Column
    private String offloadStatus;

    @Column
    private NicVirtStatus virtStatus;

    @Column
    private String description;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Transient
    private String accountUuid;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "hostNetworkInterfaceUuid", insertable = false, updatable = false)
    @NoView
    private Set<HostVirtualNetworkInterfaceVO> virtualInterfaces = new HashSet<HostVirtualNetworkInterfaceVO>();

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getBondingUuid() {
        return bondingUuid;
    }

    public void setBondingUuid(String bondingUuid) {
        this.bondingUuid = bondingUuid;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getInterfaceType() {
        return interfaceType;
    }

    public void setInterfaceType(String interfaceType) {
        this.interfaceType = interfaceType;
    }

    public Long getSpeed() {
        return speed;
    }

    public void setSpeed(Long speed) {
        this.speed = speed;
    }

    public boolean isSlaveActive() {
        return slaveActive;
    }

    public void setSlaveActive(boolean slaveActive) {
        this.slaveActive = slaveActive;
    }

    public boolean isCarrierActive() {
        return carrierActive;
    }

    public void setCarrierActive(boolean carrierActive) {
        this.carrierActive = carrierActive;
    }

    public String getIpAddresses() {
        return ipAddresses;
    }

    public void setIpAddresses(String ipAddresses) {
        this.ipAddresses = ipAddresses;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getCallBackIp() {
        return callBackIp;
    }

    public void setCallBackIp(String callBackIp) {
        this.callBackIp = callBackIp;
    }

    public String getPciDeviceAddress() {
        return pciDeviceAddress;
    }

    public void setPciDeviceAddress(String pciDeviceAddress) {
        this.pciDeviceAddress = pciDeviceAddress;
    }

    public Integer getMaxPartNum() {
        return maxPartNum;
    }

    public void setMaxPartNum(Integer maxPartNum) {
        this.maxPartNum = maxPartNum;
    }

    public String getOffloadStatus() {
        return offloadStatus;
    }

    public void setOffloadStatus(String offloadStatus) {
        this.offloadStatus = offloadStatus;
    }

    public NicVirtStatus getVirtStatus() {
        return virtStatus;
    }

    public void setVirtStatus(NicVirtStatus virtStatus) {
        this.virtStatus = virtStatus;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public Set<HostVirtualNetworkInterfaceVO> getVirtualInterfaces() {
        return virtualInterfaces;
    }

    public void setVirtualInterfaces(Set<HostVirtualNetworkInterfaceVO> virtualInterfaces) {
        this.virtualInterfaces = virtualInterfaces;
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

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }
}
