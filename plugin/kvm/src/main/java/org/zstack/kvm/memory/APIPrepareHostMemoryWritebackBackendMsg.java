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
 * Resets and binds a blank disk from GetHostMemoryWritebackBackends as the Host's writeback backend,
 * as a LongJob. The candidate, boot, pool and capacity identify the exact device to reset.
 */
@Action(category = "memoryOptimization", adminOnly = true)
@RestRequest(path = "/hosts/{uuid}/actions", method = HttpMethod.PUT,
        responseClass = APIPrepareHostMemoryWritebackBackendEvent.class, isAction = true)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 24)
public class APIPrepareHostMemoryWritebackBackendMsg extends APICreateMessage {
    @APIParam(resourceType = HostVO.class)
    private String uuid;
    @APIParam
    private String candidateId;
    @APIParam
    private String expectedHostBootId;
    @APIParam
    private String expectedPoolGeneration;
    @APIParam
    private long backendCapacityBytes;
    @APIParam
    private boolean resetConfirmed;

    public String getUuid() { return uuid; }
    public void setUuid(String value) { uuid = value; }
    public String getCandidateId() { return candidateId; }
    public void setCandidateId(String value) { candidateId = value; }
    public String getExpectedHostBootId() { return expectedHostBootId; }
    public void setExpectedHostBootId(String value) { expectedHostBootId = value; }
    public String getExpectedPoolGeneration() { return expectedPoolGeneration; }
    public void setExpectedPoolGeneration(String value) { expectedPoolGeneration = value; }
    public long getBackendCapacityBytes() { return backendCapacityBytes; }
    public void setBackendCapacityBytes(long value) { backendCapacityBytes = value; }
    public boolean isResetConfirmed() { return resetConfirmed; }
    public void setResetConfirmed(boolean value) { resetConfirmed = value; }

    MemoryBackendPreparation toPreparation() {
        MemoryBackendPreparation preparation = new MemoryBackendPreparation();
        preparation.setCandidateId(candidateId);
        preparation.setExpectedHostBootId(expectedHostBootId);
        preparation.setExpectedPoolGeneration(expectedPoolGeneration);
        preparation.setBackendCapacityBytes(backendCapacityBytes);
        preparation.setResetConfirmed(resetConfirmed);
        return preparation;
    }

    public static APIPrepareHostMemoryWritebackBackendMsg __example__() {
        APIPrepareHostMemoryWritebackBackendMsg msg = new APIPrepareHostMemoryWritebackBackendMsg();
        msg.setUuid(uuid());
        msg.setCandidateId("candidate:ddc10b8f02dc512667056d6d7c6fb8cdb13701fdb96ef373f62619a053b01541");
        msg.setExpectedHostBootId("d339587c-b015-4745-8091-d00521319861");
        msg.setExpectedPoolGeneration("none");
        msg.setBackendCapacityBytes(10737418240L);
        msg.setResetConfirmed(true);
        return msg;
    }
}
