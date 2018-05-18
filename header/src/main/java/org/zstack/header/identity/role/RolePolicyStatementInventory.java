package org.zstack.header.identity.role;

import org.zstack.header.identity.PolicyStatement;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.utils.gson.JSONObjectUtil;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = RolePolicyStatementVO.class)
public class RolePolicyStatementInventory {
    private String uuid;
    @APINoSee
    private String statementString;
    @APINoSee
    private String roleUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private PolicyStatement statement;

    public static RolePolicyStatementInventory valueOf(RolePolicyStatementVO vo) {
        RolePolicyStatementInventory inv = new RolePolicyStatementInventory();
        inv.uuid = vo.getUuid();
        inv.statementString = vo.getStatement();
        inv.roleUuid = vo.getRoleUuid();
        inv.createDate = vo.getCreateDate();
        inv.lastOpDate = vo.getLastOpDate();
        inv.statement = JSONObjectUtil.toObject(vo.getStatement(), PolicyStatement.class);
        return inv;
    }

    public static List<RolePolicyStatementInventory> valueOf(Collection<RolePolicyStatementVO> vos) {
        return vos.stream().map(RolePolicyStatementInventory::valueOf).collect(Collectors.toList());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getStatementString() {
        return statementString;
    }

    public void setStatementString(String statementString) {
        this.statementString = statementString;
    }

    public String getRoleUuid() {
        return roleUuid;
    }

    public void setRoleUuid(String roleUuid) {
        this.roleUuid = roleUuid;
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

    public PolicyStatement getStatement() {
        return statement;
    }

    public void setStatement(PolicyStatement statement) {
        this.statement = statement;
    }
}
