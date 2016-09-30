package org.zstack.ldap;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = LdapAccountRefVO.class)
@PythonClassInventory
public class LdapAccountRefInventory {
    private String uuid;
    private String ldapUid;
    private String ldapServerUuid;
    private String accountUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static LdapAccountRefInventory valueOf(LdapAccountRefVO vo) {
        LdapAccountRefInventory inv = new LdapAccountRefInventory();
        inv.setUuid(vo.getUuid());
        inv.setLdapUid(vo.getLdapUid());
        inv.setLdapServerUuid(vo.getLdapServerUuid());
        inv.setAccountUuid(vo.getAccountUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<LdapAccountRefInventory> valueOf(Collection<LdapAccountRefVO> vos) {
        List<LdapAccountRefInventory> lst = new ArrayList<>(vos.size());
        for (LdapAccountRefVO vo : vos) {
            lst.add(LdapAccountRefInventory.valueOf(vo));
        }
        return lst;
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

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getLdapUid() {
        return ldapUid;
    }

    public void setLdapUid(String ldapUid) {
        this.ldapUid = ldapUid;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getLdapServerUuid() {
        return ldapServerUuid;
    }

    public void setLdapServerUuid(String ldapServerUuid) {
        this.ldapServerUuid = ldapServerUuid;
    }
}
