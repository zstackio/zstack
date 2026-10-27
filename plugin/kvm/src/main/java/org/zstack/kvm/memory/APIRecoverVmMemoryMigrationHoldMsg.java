package org.zstack.kvm.memory;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;

import java.util.concurrent.TimeUnit;

/**
 * Releases the memory migration hold of a VM whose migration result could not be verified, as a LongJob.
 * The operator names the Host where the VM runs now, or none for a stopped VM; both Hosts are checked first.
 */
@Action(category = "memoryOptimization", adminOnly = true)
@RestRequest(path = "/vm-instances/{uuid}/actions", method = HttpMethod.PUT,
        responseClass = APIRecoverVmMemoryMigrationHoldEvent.class, isAction = true)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 1)
public class APIRecoverVmMemoryMigrationHoldMsg extends APICreateMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;
    @APIParam(resourceType = HostVO.class, required = false)
    private String confirmedHostUuid;

    public String getUuid() { return uuid; }
    public void setUuid(String value) { uuid = value; }
    public String getConfirmedHostUuid() { return confirmedHostUuid; }
    public void setConfirmedHostUuid(String value) { confirmedHostUuid = value; }

    public static APIRecoverVmMemoryMigrationHoldMsg __example__() {
        APIRecoverVmMemoryMigrationHoldMsg msg = new APIRecoverVmMemoryMigrationHoldMsg();
        msg.setUuid(uuid());
        msg.setConfirmedHostUuid(uuid());
        return msg;
    }
}
