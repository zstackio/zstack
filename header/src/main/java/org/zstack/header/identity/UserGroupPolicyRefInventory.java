package org.zstack.header.identity;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = UserGroupPolicyRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "group", inventoryClass = UserGroupInventory.class,
                foreignKey = "groupUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "policy", inventoryClass = PolicyInventory.class,
                foreignKey = "policyUuid", expandedInventoryKey = "uuid")
})
public class UserGroupPolicyRefInventory {
    private String groupUuid;
    private String policyUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static UserGroupPolicyRefInventory valueOf(UserGroupPolicyRefVO vo) {
        UserGroupPolicyRefInventory inv = new UserGroupPolicyRefInventory();
        inv.setCreateDate(vo.getCreateDate());
        inv.setGroupUuid(vo.getGroupUuid());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setPolicyUuid(vo.getPolicyUuid());
        return inv;
    }

    public static List<UserGroupPolicyRefInventory> valueOf(Collection<UserGroupPolicyRefVO> vos) {
        List<UserGroupPolicyRefInventory> invs = new ArrayList<UserGroupPolicyRefInventory>();
        for (UserGroupPolicyRefVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
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
