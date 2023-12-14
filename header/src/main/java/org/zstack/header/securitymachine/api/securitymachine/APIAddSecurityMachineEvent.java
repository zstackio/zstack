package org.zstack.header.securitymachine.api.securitymachine;

import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolType;
import org.zstack.header.securitymachine.SecurityMachineInventory;
import org.zstack.header.securitymachine.SecurityMachineState;
import org.zstack.header.securitymachine.SecurityMachineStatus;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Created by LiangHanYu on 2021/11/14 16:27
 */
@RestResponse(allTo = "inventory")
public class APIAddSecurityMachineEvent extends APIEvent {
    private SecurityMachineInventory inventory;

    public APIAddSecurityMachineEvent() {
        super(null);
    }

    public APIAddSecurityMachineEvent(String apiId) {
        super(apiId);
    }

    public SecurityMachineInventory getInventory() {
        return inventory;
    }

    public void setInventory(SecurityMachineInventory inventory) {
        this.inventory = inventory;
    }

    public static APIAddSecurityMachineEvent __example__() {
        APIAddSecurityMachineEvent event = new APIAddSecurityMachineEvent();
        SecurityMachineInventory inv = new SecurityMachineInventory();
        inv.setModel("InfoSec");
        inv.setName("inv1");
        inv.setDescription("test");
        inv.setState(SecurityMachineState.Enabled.toString());
        inv.setZoneUuid(uuid());
        inv.setSecretResourcePoolUuid(uuid());
        inv.setUuid(uuid());
        inv.setType(SecretResourcePoolType.CloudSecurityMachine.toString());
        inv.setManagementIp("192.168.1.1");
        inv.setStatus(SecurityMachineStatus.Synced.toString());
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(inv);
        return event;
    }
}
