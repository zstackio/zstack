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
 * Makes a local NVMe disk from GetHostMemorySwapDisks the Host's only swap, as a LongJob: mkswap, swapon
 * with discard, and an fstab entry by its swap signature. A disk with data is overwritten only with overwrite.
 */
@Action(category = "memoryOptimization", adminOnly = true)
@RestRequest(path = "/hosts/{uuid}/actions", method = HttpMethod.PUT,
        responseClass = APIPrepareHostMemorySwapDiskEvent.class, isAction = true)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 24)
public class APIPrepareHostMemorySwapDiskMsg extends APICreateMessage {
    @APIParam(resourceType = HostVO.class)
    private String uuid;
    @APIParam
    private String candidateId;
    @APIParam(required = false)
    private Boolean overwrite;

    public String getUuid() { return uuid; }
    public void setUuid(String value) { uuid = value; }
    public String getCandidateId() { return candidateId; }
    public void setCandidateId(String value) { candidateId = value; }
    public Boolean getOverwrite() { return overwrite; }
    public void setOverwrite(Boolean value) { overwrite = value; }

    public static APIPrepareHostMemorySwapDiskMsg __example__() {
        APIPrepareHostMemorySwapDiskMsg msg = new APIPrepareHostMemorySwapDiskMsg();
        msg.setUuid(uuid());
        msg.setCandidateId("candidate:48e5a935e1c0e0eb31aea15a379969ffe824d0a6dd0d453eb77ad81d913a5fec");
        msg.setOverwrite(false);
        return msg;
    }
}
