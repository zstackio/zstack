package org.zstack.header.identity.role.api;

import org.zstack.header.identity.role.RoleInventory;
import org.zstack.header.identity.role.RoleState;
import org.zstack.header.identity.role.RoleType;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;

/**
 * Created by kayo on 2018/7/26.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateRoleEvent extends APIEvent {
    private RoleInventory inventory;

    public APIUpdateRoleEvent() {
    }

    public APIUpdateRoleEvent(String apiId) {
        super(apiId);
    }

    public RoleInventory getInventory() {
        return inventory;
    }

    public void setInventory(RoleInventory inventory) {
        this.inventory = inventory;
    }


    public static APIUpdateRoleEvent __example__() {
        APIUpdateRoleEvent event = new APIUpdateRoleEvent();

        RoleInventory role = new RoleInventory();
        role.setName("role-1");
        role.setStatements(asList("statement for test"));
        role.setDescription("role for test");
        role.setUuid(uuid());
        role.setState(RoleState.Enabled);
        role.setType(RoleType.Customized);

        event.setInventory(role);

        return event;
    }
}
