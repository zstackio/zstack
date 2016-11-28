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

@Inventory(mappingVOClass = UserGroupVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "account", inventoryClass = AccountInventory.class,
                foreignKey = "accountUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "userRef", inventoryClass = UserGroupUserRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "groupUuid", hidden = true),
        @ExpandedQuery(expandedField = "policyRef", inventoryClass = UserGroupPolicyRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "groupUuid", hidden = true)
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "user", expandedField = "userRef.user"),
        @ExpandedQueryAlias(alias = "policy", expandedField = "policyRef.policy")
})
public class UserGroupInventory {
    private String uuid;
    private String accountUuid;
    private String name;
    private String description;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static UserGroupInventory valueOf(UserGroupVO vo) {
        UserGroupInventory inv = new UserGroupInventory();
        inv.setAccountUuid(vo.getAccountUuid());
        inv.setDescription(vo.getDescription());
        inv.setName(vo.getName());
        inv.setUuid(vo.getUuid());
        inv.setCreateDate(vo.getCreateDate());
        return inv;
    }

    public static List<UserGroupInventory> valueOf(Collection<UserGroupVO> vos) {
        List<UserGroupInventory> invs = new ArrayList<UserGroupInventory>(vos.size());
        for (UserGroupVO vo : vos) {
            invs.add(UserGroupInventory.valueOf(vo));
        }
        return invs;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
