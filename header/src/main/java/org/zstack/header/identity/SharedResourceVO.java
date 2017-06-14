package org.zstack.header.identity;

import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by frank on 7/13/2015.
 */
@Entity
@Table
@BaseResource
public class SharedResourceVO {
    @Column
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @ForeignKey(parentEntityClass = AccountVO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String ownerAccountUuid;

    @Column
    @ForeignKey(parentEntityClass = AccountVO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String receiverAccountUuid;

    @Column
    private boolean toPublic;

    @Column
    private String resourceType;

    @Column
    private String resourceUuid;

    @Column
    private Timestamp lastOpDate;

    @Column
    private Timestamp createDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOwnerAccountUuid() {
        return ownerAccountUuid;
    }

    public void setOwnerAccountUuid(String ownerAccountUuid) {
        this.ownerAccountUuid = ownerAccountUuid;
    }

    public String getReceiverAccountUuid() {
        return receiverAccountUuid;
    }

    public void setReceiverAccountUuid(String receiverAccountUuid) {
        this.receiverAccountUuid = receiverAccountUuid;
    }

    public boolean isToPublic() {
        return toPublic;
    }

    public void setToPublic(boolean toPublic) {
        this.toPublic = toPublic;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }
}
