package org.zstack.header.server;

import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = PhysicalServerRoleVO.class)
public class PhysicalServerRoleInventory implements Serializable {
    private String uuid;
    private String serverUuid;
    private String roleType;
    private String roleUuid;
    private String schedulingMode;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static PhysicalServerRoleInventory valueOf(PhysicalServerRoleVO vo) {
        PhysicalServerRoleInventory inv = new PhysicalServerRoleInventory();
        inv.setUuid(vo.getUuid());
        inv.setServerUuid(vo.getServerUuid());
        inv.setRoleType(vo.getRoleType());
        inv.setRoleUuid(vo.getRoleUuid());
        inv.setSchedulingMode(vo.getSchedulingMode() != null ? vo.getSchedulingMode().toString() : null);
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<PhysicalServerRoleInventory> valueOf(Collection<PhysicalServerRoleVO> vos) {
        List<PhysicalServerRoleInventory> invs = new ArrayList<>();
        for (PhysicalServerRoleVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getServerUuid() {
        return serverUuid;
    }

    public void setServerUuid(String serverUuid) {
        this.serverUuid = serverUuid;
    }

    public String getRoleType() {
        return roleType;
    }

    public void setRoleType(String roleType) {
        this.roleType = roleType;
    }

    public String getRoleUuid() {
        return roleUuid;
    }

    public void setRoleUuid(String roleUuid) {
        this.roleUuid = roleUuid;
    }

    public String getSchedulingMode() {
        return schedulingMode;
    }

    public void setSchedulingMode(String schedulingMode) {
        this.schedulingMode = schedulingMode;
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
