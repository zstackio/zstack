package org.zstack.header.securitymachine.api.securitymachine;

import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolType;
import org.zstack.header.securitymachine.SecurityMachineInventory;
import org.zstack.header.securitymachine.SecurityMachineState;
import org.zstack.header.securitymachine.SecurityMachineStatus;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Created by LiangHanYu on 2021/11/3 18:22
 */
@RestResponse(fieldsTo = {"all"})
public class APIUpdateSecurityMachineEvent extends APIEvent {
    private SecurityMachineInventory inventory;

    public APIUpdateSecurityMachineEvent() {
    }

    public APIUpdateSecurityMachineEvent(String apiId) {
        super(apiId);
    }

    public SecurityMachineInventory getInventory() {
        return inventory;
    }

    public void setInventory(SecurityMachineInventory inventory) {
        this.inventory = inventory;
    }

    public static APIUpdateSecurityMachineEvent __example__() {
        APIUpdateSecurityMachineEvent event = new APIUpdateSecurityMachineEvent();
        SecurityMachineInventory inv = new SecurityMachineInventory();
        inv.setUuid(uuid());
        inv.setZoneUuid(uuid());
        inv.setName("inv1");
        inv.setSecretResourcePoolUuid(uuid());
        inv.setDescription("test");
        inv.setManagementIp("192.168.0.1");
        inv.setStatus(SecurityMachineStatus.Synced.toString());
        inv.setType(SecretResourcePoolType.CloudSecurityMachine.toString());
        inv.setModel("infoSec");
        inv.setState(SecurityMachineState.Enabled.toString());
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        event.setInventory(inv);
        return event;
    }
}
