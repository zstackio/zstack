package org.zstack.header.securitymachine.secretresourcepool;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.*;
import org.zstack.header.zone.ZoneEO;
import org.zstack.header.zone.ZoneVO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by LiangHanYu on 2021/10/28 11:03
 */
@Entity
@Table
@BaseResource
@AutoDeleteTag
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = ZoneVO.class, myField = "zoneUuid", targetField = "uuid"),
        }
)
public class SecretResourcePoolVO extends ResourceVO implements ToInventory {
    @Column
    @ForeignKey(parentEntityClass = ZoneEO.class, onDeleteAction = ForeignKey.ReferenceOption.RESTRICT)
    private String zoneUuid;

    @Column
    private String name;

    @Column
    @Enumerated(EnumType.STRING)
    private SecretResourcePoolType type;

    @Column
    private String description;

    @Column
    @Enumerated(EnumType.STRING)
    private SecretResourcePoolState state;

    /**
     * @desc the model of the security machine under the secret resource pool
     * @choices - InfoSec
     */
    @Column
    private String model;

    /**
     * @desc used to set the time to periodically check the connection status of the security machine
     */
    @Column
    private Integer heartbeatInterval;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public SecretResourcePoolVO() {
    }

    protected SecretResourcePoolVO(SecretResourcePoolVO vo) {
        this.setModel(vo.getModel());
        this.setName(vo.getName());
        this.setDescription(vo.getDescription());
        this.setState(vo.getState());
        this.setZoneUuid(vo.getZoneUuid());
        this.setUuid(vo.getUuid());
        this.setType(vo.getType());
        this.setHeartbeatInterval(vo.getHeartbeatInterval());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public Integer getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(Integer heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
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

    public SecretResourcePoolType getType() {
        return type;
    }

    public void setType(SecretResourcePoolType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SecretResourcePoolState getState() {
        return state;
    }

    public void setState(SecretResourcePoolState state) {
        this.state = state;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
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
