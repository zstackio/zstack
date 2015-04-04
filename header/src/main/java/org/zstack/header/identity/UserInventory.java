package org.zstack.header.identity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Inventory(mappingVOClass = UserVO.class)
@PythonClassInventory
public class UserInventory {
    private String uuid;
    private String accountUuid;
    private String name;
    private String securityKey;
    private String token;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private List<UserGroupInventory> groups;
    private List<PolicyInventory> policies;
    
    public static UserInventory valueOf(UserVO vo) {
        UserInventory inv = new UserInventory();
        inv.setUuid(vo.getUuid());
        inv.setAccountUuid(vo.getAccountUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setName(vo.getName());
        inv.setSecurityKey(vo.getSecurityKey());
        inv.setToken(vo.getToken());
        inv.setPolicies(PolicyInventory.valueOf(vo.getPolicies()));
        inv.setGroups(UserGroupInventory.valueOf(vo.getGroups()));
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }
    
    public static List<UserInventory> valueOf(Collection<UserVO> vos) {
        List<UserInventory> invs = new ArrayList<UserInventory>(vos.size());
        for (UserVO vo : vos) {
            invs.add(UserInventory.valueOf(vo));
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
    public String getSecurityKey() {
        return securityKey;
    }
    public void setSecurityKey(String securityKey) {
        this.securityKey = securityKey;
    }
    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }

    public List<PolicyInventory> getPolicies() {
        return policies;
    }

    public void setPolicies(List<PolicyInventory> policies) {
        this.policies = policies;
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

    public List<UserGroupInventory> getGroups() {
        return groups;
    }

    public void setGroups(List<UserGroupInventory> groups) {
        this.groups = groups;
    }
}
