package org.zstack.header.identity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.ExpandedQueryAlias;
import org.zstack.header.query.ExpandedQueryAliases;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = UserVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "account", inventoryClass = AccountInventory.class,
                foreignKey = "accountUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "groupRef", inventoryClass = UserGroupUserRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "userUuid", hidden = true),
        @ExpandedQuery(expandedField = "policyRef", inventoryClass = UserPolicyRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "userUuid", hidden = true)
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "group", expandedField = "groupRef.group"),
        @ExpandedQueryAlias(alias = "policy", expandedField = "policyRef.policy")
})
public class UserInventory {
    private String uuid;
    private String accountUuid;
    private String name;
    private String description;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static UserInventory valueOf(UserVO vo) {
        UserInventory inv = new UserInventory();
        inv.setUuid(vo.getUuid());
        inv.setAccountUuid(vo.getAccountUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setName(vo.getName());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setDescription(vo.getDescription());
        return inv;
    }

    public static List<UserInventory> valueOf(Collection<UserVO> vos) {
        List<UserInventory> invs = new ArrayList<UserInventory>(vos.size());
        for (UserVO vo : vos) {
            invs.add(UserInventory.valueOf(vo));
        }
        return invs;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
