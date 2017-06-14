package org.zstack.header.identity;

import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.Index;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
public class AccountResourceRefVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = AccountVO.class, parentKey = "uuid", onDeleteAction = ReferenceOption.CASCADE)
    private String accountUuid;

    @Column
    @ForeignKey(parentEntityClass = AccountVO.class, parentKey = "uuid", onDeleteAction = ReferenceOption.CASCADE)
    private String ownerAccountUuid;

    @Column
    @Index
    private String resourceUuid;

    // may be deprecated later, should not rely on it
    @Column
    @Index
    private String resourceType;

    @Column
    private int permission;

    @Column
    private boolean isShared;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public int getPermission() {
        return permission;
    }

    public void setPermission(int permission) {
        this.permission = permission;
    }

    public boolean isShared() {
        return isShared;
    }

    public void setShared(boolean isShared) {
        this.isShared = isShared;
    }

    public String getOwnerAccountUuid() {
        return ownerAccountUuid;
    }

    public void setOwnerAccountUuid(String ownerAccountUuid) {
        this.ownerAccountUuid = ownerAccountUuid;
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

    public static AccountResourceRefVO newOwn(String accountUuid, String resourceUuid, Class<?> resourceClass) {
        AccountResourceRefVO ref = new AccountResourceRefVO();
        ref.setAccountUuid(accountUuid);
        ref.setResourceType(resourceClass.getSimpleName());
        ref.setResourceUuid(resourceUuid);
        ref.setPermission(AccountConstant.RESOURCE_PERMISSION_WRITE);
        ref.setOwnerAccountUuid(accountUuid);
        ref.setShared(false);
        return ref;
    }
}
