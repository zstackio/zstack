package org.zstack.header.securitymachine.api.securitymachine;

import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolType;
import org.zstack.header.securitymachine.SecurityMachineInventory;
import org.zstack.header.securitymachine.SecurityMachineState;
import org.zstack.header.securitymachine.SecurityMachineStatus;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by LiangHanYu on 2021/11/3 17:27
 */
@RestResponse(allTo = "inventories")
public class APIQuerySecurityMachineReply extends APIQueryReply {
    private List<SecurityMachineInventory> inventories;

    public List<SecurityMachineInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SecurityMachineInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQuerySecurityMachineReply __example__() {
        APIQuerySecurityMachineReply reply = new APIQuerySecurityMachineReply();
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
        reply.setInventories(asList(inv));
        reply.setSuccess(true);
        return reply;
    }
}