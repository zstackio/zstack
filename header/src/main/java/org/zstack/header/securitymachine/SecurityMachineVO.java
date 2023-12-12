package org.zstack.header.securitymachine;

import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolType;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.*;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.Index;
import org.zstack.header.zone.ZoneEO;
import org.zstack.header.zone.ZoneVO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by LiangHanYu on 2021/10/28 11:58
 */
@Entity
@Table
@AutoDeleteTag
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = SecretResourcePoolVO.class, myField = "secretResourcePoolUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = ZoneVO.class, myField = "zoneUuid", targetField = "uuid"),
        }
)
public class SecurityMachineVO extends ResourceVO implements ToInventory {
    @Column
    @ForeignKey(parentEntityClass = ZoneEO.class, onDeleteAction = ForeignKey.ReferenceOption.RESTRICT)
    private String zoneUuid;

    @Column
    private String name;

    @Column
    @Index
    @ForeignKey(parentEntityClass = SecretResourcePoolVO.class, onDeleteAction = ForeignKey.ReferenceOption.RESTRICT)
    private String secretResourcePoolUuid;

    @Column
    private String description;

    @Column
    private String managementIp;

    @Column
    @Enumerated(EnumType.STRING)
    private SecretResourcePoolType type;

    /**
     * @desc the model of the security machine
     * @choices - InfoSec
     */
    @Column
    private String model;

    @Column
    @Enumerated(EnumType.STRING)
    private SecurityMachineState state;

    @Column
    @Enumerated(EnumType.STRING)
    private SecurityMachineStatus status;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public SecurityMachineVO() {
    }

    protected SecurityMachineVO(SecurityMachineVO vo) {
        this.setModel(vo.getModel());
        this.setName(vo.getName());
        this.setDescription(vo.getDescription());
        this.setStatus(vo.getStatus());
        this.setState(vo.getState());
        this.setZoneUuid(vo.getZoneUuid());
        this.setUuid(vo.getUuid());
        this.setType(vo.getType());
        this.setManagementIp(vo.getManagementIp());
        this.setSecretResourcePoolUuid(vo.getSecretResourcePoolUuid());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSecretResourcePoolUuid() {
        return secretResourcePoolUuid;
    }

    public void setSecretResourcePoolUuid(String secretResourcePoolUuid) {
        this.secretResourcePoolUuid = secretResourcePoolUuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getManagementIp() {
        return managementIp;
    }

    public void setManagementIp(String managementIp) {
        this.managementIp = managementIp;
    }

    public SecretResourcePoolType getType() {
        return type;
    }

    public void setType(SecretResourcePoolType type) {
        this.type = type;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public SecurityMachineState getState() {
        return state;
    }

    public void setState(SecurityMachineState state) {
        this.state = state;
    }

    public SecurityMachineStatus getStatus() {
        return status;
    }

    public void setStatus(SecurityMachineStatus status) {
        this.status = status;
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
