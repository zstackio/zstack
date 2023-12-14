package org.zstack.header.securitymachine.api.secretresourcepool;

import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolInventory;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolState;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolType;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Created by LiangHanYu on 2021/11/4 11:24
 */
@RestResponse(allTo = "inventory")
public class APICreateSecretResourcePoolEvent extends APIEvent {
    private SecretResourcePoolInventory inventory;

    public APICreateSecretResourcePoolEvent() {
        super(null);
    }

    public APICreateSecretResourcePoolEvent(String apiId) {
        super(apiId);
    }

    public SecretResourcePoolInventory getInventory() {
        return inventory;
    }

    public void setInventory(SecretResourcePoolInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateSecretResourcePoolEvent __example__() {
        APICreateSecretResourcePoolEvent event = new APICreateSecretResourcePoolEvent();
        SecretResourcePoolInventory hi = new SecretResourcePoolInventory();
        hi.setModel("InfoSec");
        hi.setName("inv1");
        hi.setDescription("test");
        hi.setState(SecretResourcePoolState.Activated.toString());
        hi.setZoneUuid(uuid());
        hi.setUuid(uuid());
        hi.setHeartbeatInterval(6);
        hi.setType(SecretResourcePoolType.CloudSecurityMachine.toString());
        hi.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        hi.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(hi);
        return event;
    }

}
