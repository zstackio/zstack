package org.zstack.header.identity;

import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 */
@Inventory(mappingVOClass = AccountResourceRefVO.class)
public class AccountResourceRefInventory {
    @APINoSee
    private Long id;
    private String accountUuid;
    @APINoSee
    private String ownerAccountUuid;
    private String resourceUuid;
    private String resourceType;
    @APINoSee
    private Integer permission;
    @APINoSee
    private Boolean isShared;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static AccountResourceRefInventory valueOf(AccountResourceRefVO vo) {
        AccountResourceRefInventory inv = new AccountResourceRefInventory();
        inv.setAccountUuid(vo.getAccountUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setId(vo.getId());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setOwnerAccountUuid(vo.getOwnerAccountUuid());
        inv.setPermission(vo.getPermission());
        inv.setResourceType(vo.getResourceType());
        inv.setResourceUuid(vo.getResourceUuid());
        inv.setShared(vo.isShared());
        return inv;
    }

    public static List<AccountResourceRefInventory> valueOf(Collection<AccountResourceRefVO> vos) {
        List<AccountResourceRefInventory> invs = new ArrayList<AccountResourceRefInventory>();
        for (AccountResourceRefVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
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

    public String getOwnerAccountUuid() {
        return ownerAccountUuid;
    }

    public void setOwnerAccountUuid(String ownerAccountUuid) {
        this.ownerAccountUuid = ownerAccountUuid;
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
