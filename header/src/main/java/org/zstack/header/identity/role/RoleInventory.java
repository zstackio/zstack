package org.zstack.header.identity.role;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = RoleVO.class)
public class RoleInventory {
    private String uuid;
    private String name;
    private String description;
    private RoleType type;
    private RoleState state;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private List<RolePolicyStatementInventory> statements;

    public static RoleInventory valueOf(RoleVO vo) {
        RoleInventory inv = new RoleInventory();
        inv.uuid = vo.getUuid();
        inv.name = vo.getName();
        inv.type = vo.getType();
        inv.state = vo.getState();
        inv.description = vo.getDescription();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();
        inv.statements = RolePolicyStatementInventory.valueOf(vo.getStatements());
        return inv;
    }

    public static List<RoleInventory> valueOf(Collection<RoleVO> vos) {
        return vos.stream().map(RoleInventory::valueOf).collect(Collectors.toList());
    }

    public RoleState getState() {
        return state;
    }

    public void setState(RoleState state) {
        this.state = state;
    }

    public RoleType getType() {
        return type;
    }

    public void setType(RoleType type) {
        this.type = type;
    }

    public List<RolePolicyStatementInventory> getStatements() {
        return statements;
    }

    public void setStatements(List<RolePolicyStatementInventory> statements) {
        this.statements = statements;
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
