package org.zstack.header.securitymachine.api.secretresourcepool;

import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolType;
import org.zstack.header.securitymachine.SecurityMachineInventory;
import org.zstack.header.securitymachine.SecurityMachineState;
import org.zstack.header.securitymachine.SecurityMachineStatus;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.CollectionDSL;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by LiangHanYu on 2021/11/9 19:26
 */
@RestResponse(allTo = "inventories")
public class APISetSecurityMachineKeyEvent extends APIEvent {
    List<SecurityMachineInventory> inventories = new ArrayList<>();

    public List<SecurityMachineInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SecurityMachineInventory> inventories) {
        this.inventories = inventories;
    }

    public APISetSecurityMachineKeyEvent() {
        super(null);
    }

    public APISetSecurityMachineKeyEvent(String apiId) {
        super(apiId);
    }

    public static APISetSecurityMachineKeyEvent __example__() {
        APISetSecurityMachineKeyEvent event = new APISetSecurityMachineKeyEvent();
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
        event.setInventories(CollectionDSL.list(inv));
        return event;
    }
}
