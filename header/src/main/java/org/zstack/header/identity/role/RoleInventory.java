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
    private String identity;
    private String rootUuid;
    private RoleType type;
    private RoleState state;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private List<RolePolicyStatementInventory> statements;
    private List<RolePolicyRefInventory> policies;

    protected RoleInventory(RoleVO vo) {
        this.setCreateDate(vo.getCreateDate());
        this.setDescription(vo.getDescription());
        this.setLastOpDate(vo.getLastOpDate());
        this.setType(vo.getType());
        this.setName(vo.getName());
        this.setState(vo.getState());
        this.setUuid(vo.getUuid());
        this.setIdentity(vo.getIdentity());
        this.setRootUuid(vo.getRootUuid());
        this.setStatements(RolePolicyStatementInventory.valueOf(vo.getStatements()));
        this.setPolicies(RolePolicyRefInventory.valueOf(vo.getPolicies()));
    }

    public RoleInventory() {
    }

    public static RoleInventory valueOf(RoleVO vo) {
        RoleInventory inv = new RoleInventory();
        inv.uuid = vo.getUuid();
        inv.name = vo.getName();
        inv.type = vo.getType();
        inv.state = vo.getState();
        inv.description = vo.getDescription();
        inv.identity = vo.getIdentity();
        inv.rootUuid = vo.getRootUuid();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();
        inv.statements = RolePolicyStatementInventory.valueOf(vo.getStatements());
        inv.policies = RolePolicyRefInventory.valueOf(vo.getPolicies());
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

    public List<RolePolicyRefInventory> getPolicies() {
        return policies;
    }

    public void setPolicies(List<RolePolicyRefInventory> policies) {
        this.policies = policies;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getRootUuid() {
        return rootUuid;
    }

    public void setRootUuid(String rootUuid) {
        this.rootUuid = rootUuid;
    }
}
