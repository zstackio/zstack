package org.zstack.kvm.memory;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;
import java.util.concurrent.TimeUnit;

/**
 * Reads the data on the Host's swap disk back into memory and stops using the disk, as a LongJob:
 * swapoff, removing its fstab entry and its signature. Refused while memory cannot take the swapped data back.
 */
@Action(category = "memoryOptimization", adminOnly = true)
@RestRequest(path = "/hosts/{uuid}/actions", method = HttpMethod.PUT,
        responseClass = APIDisableHostMemorySwapDiskEvent.class, isAction = true)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 24)
public class APIDisableHostMemorySwapDiskMsg extends APICreateMessage {
    @APIParam(resourceType = HostVO.class)
    private String uuid;

    public String getUuid() { return uuid; }
    public void setUuid(String value) { uuid = value; }

    public static APIDisableHostMemorySwapDiskMsg __example__() {
        APIDisableHostMemorySwapDiskMsg msg = new APIDisableHostMemorySwapDiskMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
