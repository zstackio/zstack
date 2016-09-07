package org.zstack.ldap;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = AccountVO.class)
@PythonClassInventory
public class LdapServerInventory {
    private String uuid;
    private String name;
    private String description;
    private String type;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static LdapServerInventory valueOf(AccountVO vo) {
        LdapServerInventory inv = new LdapServerInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setType(vo.getType().toString());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<LdapServerInventory> valueOf(Collection<AccountVO> vos) {
        List<LdapServerInventory> lst = new ArrayList<LdapServerInventory>(vos.size());
        for (AccountVO vo : vos) {
            lst.add(LdapServerInventory.valueOf(vo));
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
