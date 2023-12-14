package org.zstack.header.securitymachine.api.secretresourcepool;

import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolInventory;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolState;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolType;
import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by LiangHanYu on 2021/11/3 17:06
 */

@RestResponse(allTo = "inventories")
public class APIQuerySecretResourcePoolReply extends APIQueryReply {
    private List<SecretResourcePoolInventory> inventories;

    public List<SecretResourcePoolInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<SecretResourcePoolInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQuerySecretResourcePoolReply __example__() {
        APIQuerySecretResourcePoolReply reply = new APIQuerySecretResourcePoolReply();
        SecretResourcePoolInventory inv = new SecretResourcePoolInventory();
        inv.setModel("InfoSec");
        inv.setName("inv1");
        inv.setDescription("test");
        inv.setState(SecretResourcePoolState.Activated.toString());
        inv.setZoneUuid(uuid());
        inv.setUuid(uuid());
        inv.setType(SecretResourcePoolType.CloudSecurityMachine.toString());
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        reply.setInventories(asList(inv));
        reply.setSuccess(true);
        return reply;
    }
}
