package org.zstack.header.identity.role.api;

import org.zstack.header.identity.PolicyStatement;
import org.zstack.header.identity.StatementEffect;
import org.zstack.header.identity.role.RoleInventory;
import org.zstack.header.identity.role.RolePolicyStatementInventory;
import org.zstack.header.identity.role.RoleState;
import org.zstack.header.identity.role.RoleType;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

import static java.util.Arrays.asList;

@RestResponse(allTo = "inventory")
public class APIChangeRoleStateEvent extends APIEvent {
    private RoleInventory inventory;

    public APIChangeRoleStateEvent() {
    }

    public APIChangeRoleStateEvent(String apiId) {
        super(apiId);
    }

    public RoleInventory getInventory() {
        return inventory;
    }

    public void setInventory(RoleInventory inventory) {
        this.inventory = inventory;
    }

    public static APIChangeRoleStateEvent __example__() {
        APIChangeRoleStateEvent event = new APIChangeRoleStateEvent();

        RoleInventory role = new RoleInventory();
        role.setName("role-1");

        RolePolicyStatementInventory inv = new RolePolicyStatementInventory();
        PolicyStatement statement = new PolicyStatement();
        statement.setEffect(StatementEffect.Allow);
        statement.setActions(asList("org.zstack.header.vm.APICreateVmInstanceMsg"));
        statement.setName("statement for test");

        inv.setUuid(uuid());
        inv.setStatement(statement);
        inv.setCreateDate(new Timestamp(System.currentTimeMillis()));
        inv.setLastOpDate(new Timestamp(System.currentTimeMillis()));
        inv.setRoleUuid(uuid());

        role.setStatements(asList(inv));
        role.setDescription("role for test");
        role.setUuid(uuid());
        role.setState(RoleState.Enabled);
        role.setType(RoleType.Customized);

        event.setInventory(role);

        return event;
    }
}
