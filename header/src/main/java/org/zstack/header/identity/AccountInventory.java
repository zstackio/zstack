package org.zstack.header.identity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = AccountVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "user", inventoryClass = UserInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "accountUuid"),
        @ExpandedQuery(expandedField = "group", inventoryClass = UserGroupInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "accountUuid"),
        @ExpandedQuery(expandedField = "policy", inventoryClass = PolicyInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "accountUuid"),
        @ExpandedQuery(expandedField = "quota", inventoryClass = QuotaInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "identityUuid")
})
@PythonClassInventory
public class AccountInventory {
    private String uuid;
    private String name;
    private String description;
    private String type;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static AccountInventory valueOf(AccountVO vo) {
        AccountInventory inv = new AccountInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setType(vo.getType().toString());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<AccountInventory> valueOf(Collection<AccountVO> vos) {
        List<AccountInventory> lst = new ArrayList<AccountInventory>(vos.size());
        for (AccountVO vo : vos) {
            lst.add(AccountInventory.valueOf(vo));
        }
        return lst;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
