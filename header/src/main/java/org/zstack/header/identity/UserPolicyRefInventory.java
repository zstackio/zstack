package org.zstack.header.identity;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = UserPolicyRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "user", inventoryClass = UserInventory.class,
                foreignKey = "userUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "policy", inventoryClass = PolicyInventory.class,
                foreignKey = "policyUuid", expandedInventoryKey = "uuid")
})
public class UserPolicyRefInventory {
    private long id;
    private String userUuid;
    private String policyUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    
    public UserPolicyRefInventory valueOf(UserPolicyRefVO vo) {
        UserPolicyRefInventory inv = new UserPolicyRefInventory();
        inv.setId(vo.getId());
        inv.setUserUuid(vo.getUserUuid());
        inv.setPolicyUuid(vo.getPolicyUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public List<UserPolicyRefInventory> valueOf(Collection<UserPolicyRefVO> vos) {
        List<UserPolicyRefInventory> invs = new ArrayList<UserPolicyRefInventory>();
        for (UserPolicyRefVO vo : vos) {
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
    public String getUserUuid() {
        return userUuid;
    }
    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }
    public String getPolicyUuid() {
        return policyUuid;
    }
    public void setPolicyUuid(String policyUuid) {
        this.policyUuid = policyUuid;
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
