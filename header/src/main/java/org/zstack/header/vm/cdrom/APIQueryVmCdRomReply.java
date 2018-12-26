package org.zstack.header.vm.cdrom;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import java.sql.Timestamp;
import java.util.List;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;

@RestResponse(allTo = "inventories")
public class APIQueryVmCdRomReply extends APIQueryReply {
    private List<VmCdRomInventory> inventories;

    public List<VmCdRomInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VmCdRomInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryVmCdRomReply __example__() {
        APIQueryVmCdRomReply reply = new APIQueryVmCdRomReply();

        VmCdRomInventory vm = new VmCdRomInventory();
        vm.setName("Test-VM");
        vm.setUuid(uuid());
        vm.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        vm.setDescription("web server VM");
        vm.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        reply.setInventories(asList(vm));
        return reply;
    }
}
