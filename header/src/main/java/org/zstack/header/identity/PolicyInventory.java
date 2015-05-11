
package org.zstack.header.identity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Inventory(mappingVOClass = PolicyVO.class)
@PythonClassInventory
public class PolicyInventory {
    private String uuid;
    private String name;
    private String accountUuid;
    private String type;
    private String data;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    
    public static PolicyInventory valueOf(PolicyVO vo) {
        PolicyInventory inv = new PolicyInventory();
        inv.setName(vo.getName());
        inv.setUuid(vo.getUuid());
        inv.setAccountUuid(vo.getAccountUuid());
        inv.setData(vo.getData());
        inv.setType(vo.getType().toString());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }
    
    public static List<PolicyInventory> valueOf(Collection<PolicyVO> vos) {
        List<PolicyInventory> invs = new ArrayList<PolicyInventory>(vos.size());
        for (PolicyVO vo : vos) {
            invs.add(PolicyInventory.valueOf(vo));
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
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getData() {
        return data;
    }
    public void setData(String data) {
        this.data = data;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
