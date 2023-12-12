package org.zstack.header.securitymachine.api.secretresourcepool;

import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolInventory;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolState;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolType;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Created by LiangHanYu on 2021/11/3 17:37
 */
@RestResponse(allTo = "inventory")
public class APIUpdateSecretResourcePoolEvent extends APIEvent {
    private SecretResourcePoolInventory inventory;

    public APIUpdateSecretResourcePoolEvent() {
        super(null);
    }

    public APIUpdateSecretResourcePoolEvent(String apiId) {
        super(apiId);
    }

    public SecretResourcePoolInventory getInventory() {
        return inventory;
    }

    public void setInventory(SecretResourcePoolInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateSecretResourcePoolEvent __example__() {
        APIUpdateSecretResourcePoolEvent event = new APIUpdateSecretResourcePoolEvent();
        SecretResourcePoolInventory inv = new SecretResourcePoolInventory();
        inv.setModel("InfoSec");
        inv.setName("inv1");
        inv.setDescription("test");
        inv.setState(SecretResourcePoolState.Activated.toString());
        inv.setZoneUuid(uuid());
        inv.setUuid(uuid());
        inv.setHeartbeatInterval(6);
        inv.setType(SecretResourcePoolType.CloudSecurityMachine.toString());
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(inv);
        return event;
    }
}
