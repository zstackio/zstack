package org.zstack.network.service.virtualrouter;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;
import java.util.List;

/**
 */
@RestResponse(allTo = "inventories")
public class APIGetVipUsedPortsReply extends APIReply {

    public static class VipPortRangeInventory {
        private String vipUuid;
        private String protcol;
        private List<String> usedPorts;

        public String getVipUuid() {
            return vipUuid;
        }

        public void setVipUuid(String vipUuid) {
            this.vipUuid = vipUuid;
        }

        public String getProtcol() {
            return protcol;
        }

        public void setProtcol(String protcol) {
            this.protcol = protcol;
        }

        public List<String> getUsedPorts() {
            return usedPorts;
        }

        public void setUsedPorts(List<String> usedPorts) {
            this.usedPorts = usedPorts;
        }
    }

    List<VipPortRangeInventory> inventories;

    public List<VipPortRangeInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<VipPortRangeInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIGetVipUsedPortsReply __example__() {
        APIGetVipUsedPortsReply reply = new APIGetVipUsedPortsReply();
        APIGetVipUsedPortsReply.VipPortRangeInventory inv = new APIGetVipUsedPortsReply.VipPortRangeInventory();
        inv.setVipUuid(uuid());
        inv.setProtcol("tcp");
        String[] array = {"100", "200", "201", "202", "204", "1000"};
        inv.setUsedPorts(Arrays.asList(array));
        reply.setInventories(Arrays.asList(inv));
        reply.setSuccess(true);
        return reply;
    }
}
