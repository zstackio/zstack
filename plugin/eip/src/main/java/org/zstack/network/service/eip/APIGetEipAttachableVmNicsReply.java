package org.zstack.network.service.eip;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.vm.VmNicInventory;

import java.sql.Timestamp;
import java.util.List;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;

/**
 */
@RestResponse(allTo = "inventories")
public class APIGetEipAttachableVmNicsReply extends APIReply {
    private List<VmNicInventory> inventories;

    public List<VmNicInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmNicInventory> inventories) {
        this.inventories = inventories;
    }
 
    public static APIGetEipAttachableVmNicsReply __example__() {
        APIGetEipAttachableVmNicsReply reply = new APIGetEipAttachableVmNicsReply();

        VmNicInventory nic = new VmNicInventory();
        nic.setVmInstanceUuid(uuid());
        nic.setCreateDate(new Timestamp(System.currentTimeMillis()));
        nic.setLastOpDate(new Timestamp(System.currentTimeMillis()));
        nic.setDeviceId(0);
        nic.setGateway("192.168.1.1");
        nic.setIp("192.168.1.10");
        nic.setL3NetworkUuid(uuid());
        nic.setNetmask("255.255.255.0");
        nic.setMac("00:0c:29:bd:99:fc");
        nic.setUsedIpUuid(uuid());
        nic.setUuid(uuid());

        reply.setInventories(asList(nic));
        return reply;
    }

}
