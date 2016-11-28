package org.zstack.header.identity;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = UserGroupUserRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "user", inventoryClass = UserInventory.class,
                foreignKey = "userUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "group", inventoryClass = UserGroupInventory.class,
                foreignKey = "groupUuid", expandedInventoryKey = "uuid")
})
public class UserGroupUserRefInventory {
    private String userUuid;
    private String groupUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static UserGroupUserRefInventory valueOf(UserGroupUserRefVO vo) {
        UserGroupUserRefInventory inv = new UserGroupUserRefInventory();
        inv.setUserUuid(vo.getUserUuid());
        inv.setGroupUuid(vo.getGroupUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<UserGroupUserRefInventory> valueOf(Collection<UserGroupUserRefVO> vos) {
        List<UserGroupUserRefInventory> invs = new ArrayList<UserGroupUserRefInventory>();
        for (UserGroupUserRefVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
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
