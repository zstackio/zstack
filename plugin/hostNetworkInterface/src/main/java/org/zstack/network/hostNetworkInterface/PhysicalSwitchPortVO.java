package org.zstack.network.hostNetworkInterface;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Physical Switch Port Value Object
 */
@Entity
@Table(name = "PhysicalSwitchPortVO")
@AutoDeleteTag
public class PhysicalSwitchPortVO extends ResourceVO implements ToInventory, OwnedByAccount {
    
    @Column
    private String name;
    
    @Column
    private String description;
    
    @Column
    private String ethTrunkName;
    
    @Column
    private String portType;

    @Column
    @ForeignKey(parentEntityClass = PhysicalSwitchVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String switchUuid;

    @Column
    @ForeignKey(parentEntityClass = HostNetworkInterfaceVO.class, parentKey = "uuid")
    private String peerInterfaceUuid;

    @Column
    private String sdnControllerUuid;
    
    @Column
    private Timestamp createDate;
    
    @Column
    private Timestamp lastOpDate;
    
    @Transient
    private String accountUuid;
    
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

    public String getEthTrunkName() {
        return ethTrunkName;
    }

    public void setEthTrunkName(String ethTrunkName) {
        this.ethTrunkName = ethTrunkName;
    }

    public String getPortType() {
        return portType;
    }

    public void setPortType(String portType) {
        this.portType = portType;
    }

    public String getPeerInterfaceUuid() {
        return peerInterfaceUuid;
    }

    public void setPeerInterfaceUuid(String peerInterfaceUuid) {
        this.peerInterfaceUuid = peerInterfaceUuid;
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

    public String getSwitchUuid() {
        return switchUuid;
    }

    public void setSwitchUuid(String switchUuid) {
        this.switchUuid = switchUuid;
    }

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getSdnControllerUuid() {
        return sdnControllerUuid;
    }

    public void setSdnControllerUuid(String sdnControllerUuid) {
        this.sdnControllerUuid = sdnControllerUuid;
    }

    public void copyFromAnother(PhysicalSwitchPortVO vo) {
        this.name = vo.name;
        this.description = vo.description;
        this.ethTrunkName = vo.ethTrunkName;
        this.portType = vo.portType;
        this.switchUuid = vo.switchUuid;
        this.sdnControllerUuid = vo.sdnControllerUuid;
        if (vo.getPeerInterfaceUuid() != null) {
            this.peerInterfaceUuid = vo.getPeerInterfaceUuid();
        }
    }
}
