package org.zstack.header.identity.role;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = RolePolicyRefVO.class)
public class RolePolicyRefInventory {
    private String roleUuid;
    private String policyUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static RolePolicyRefInventory valueOf(RolePolicyRefVO vo) {
        RolePolicyRefInventory inv = new RolePolicyRefInventory();
        inv.roleUuid = vo.getRoleUuid();
        inv.policyUuid = vo.getPolicyUuid();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();
        return inv;
    }

    public static List<RolePolicyRefInventory> valueOf(Collection<RolePolicyRefVO> vos) {
        return vos.stream().map(RolePolicyRefInventory::valueOf).collect(Collectors.toList());
    }

    public String getRoleUuid() {
        return roleUuid;
    }

    public void setRoleUuid(String roleUuid) {
        this.roleUuid = roleUuid;
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
