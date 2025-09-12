package org.zstack.network.hostNetworkInterface;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

/**
 * Physical Switch Value Object
 */
@Entity
@Table
@AutoDeleteTag
public class PhysicalSwitchVO extends ResourceVO implements ToInventory, OwnedByAccount {

    @Column
    private String name;
    
    @Column
    private String description;
    
    @Column
    private String ip;
    
    @Column
    private String mac;
    
    @Column
    private String mode;
    
    @Column
    private String softwareVersion;

    @Column
    private String sdnControllerUuid;
    
    @Column
    private Timestamp createDate;
    
    @Column
    private Timestamp lastOpDate;
    
    @Transient
    private String accountUuid;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "switchUuid", insertable = false, updatable = false)
    @NoView
    private Set<PhysicalSwitchPortVO> ports = new HashSet<PhysicalSwitchPortVO>();

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
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

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getSoftwareVersion() {
        return softwareVersion;
    }

    public void setSoftwareVersion(String softwareVersion) {
        this.softwareVersion = softwareVersion;
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

    public Set<PhysicalSwitchPortVO> getPorts() {
        return ports;
    }

    public void setPorts(Set<PhysicalSwitchPortVO> ports) {
        this.ports = ports;
    }

    public String getSdnControllerUuid() {
        return sdnControllerUuid;
    }

    public void setSdnControllerUuid(String sdnControllerUuid) {
        this.sdnControllerUuid = sdnControllerUuid;
    }

    public void copyFromAnother(PhysicalSwitchVO vo) {
        this.name = vo.name;
        this.description = vo.description;
        this.ip = vo.ip;
        this.mac = vo.mac;
        this.mode = vo.mode;
        this.softwareVersion = vo.softwareVersion;
        this.sdnControllerUuid = vo.sdnControllerUuid;
    }
}
