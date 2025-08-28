package org.zstack.sdnController.header;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;
import org.zstack.header.zone.ZoneEO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by shixin.ruan on 09/17/2019.
 */
@Entity
@Table
@BaseResource
public class SdnControllerVO extends ResourceVO implements OwnedByAccount, ToInventory {
    @Transient
    private String accountUuid;

    @Column
    private String vendorType;

    @Column
    private String vendorVersion;

    @Column
    private String name;

    @Column
    private String description;

    @Column
    private String ip;

    @Column
    private String username;

    @Column
    private String password;

    @OneToMany(fetch=FetchType.EAGER)
    @JoinColumn(name="sdnControllerUuid", insertable=false, updatable=false)
    @NoView
    private Set<HardwareL2VxlanNetworkPoolVO> vxlanPools = new HashSet<HardwareL2VxlanNetworkPoolVO>();

    @OneToMany(fetch=FetchType.EAGER)
    @JoinColumn(name="sdnControllerUuid", insertable=false, updatable=false)
    @NoView
    private Set<SdnControllerHostRefVO> hostRefVOS = new HashSet<SdnControllerHostRefVO>();

    @Column
    @Enumerated(EnumType.STRING)
    private SdnControllerStatus status;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public SdnControllerVO() {
    }

    public String getVendorType() {
        return vendorType;
    }

    public void setVendorType(String vendorType) {
        this.vendorType = vendorType;
    }

    public String getVendorVersion() {
        return vendorVersion;
    }

    public void setVendorVersion(String vendorVersion) {
        this.vendorVersion = vendorVersion;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public Set<HardwareL2VxlanNetworkPoolVO> getVxlanPools() {
        return vxlanPools;
    }

    public void setVxlanPools(Set<HardwareL2VxlanNetworkPoolVO> vxlanPools) {
        this.vxlanPools = vxlanPools;
    }

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public SdnControllerStatus getStatus() {
        return status;
    }

    public void setStatus(SdnControllerStatus status) {
        this.status = status;
    }

    public Set<SdnControllerHostRefVO> getHostRefVOS() {
        return hostRefVOS;
    }

    public void setHostRefVOS(Set<SdnControllerHostRefVO> hostRefVOS) {
        this.hostRefVOS = hostRefVOS;
    }
}
