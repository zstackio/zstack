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
 * Confirms recovery of a Host whose last activation or configuration result is unknown, as a LongJob.
 * The parameters are the uncertainRecovery values from the Host state, passed back unchanged.
 */
@Action(category = "memoryOptimization", adminOnly = true)
@RestRequest(path = "/hosts/{uuid}/actions", method = HttpMethod.PUT,
        responseClass = APIRecoverHostMemoryOptimizationEvent.class, isAction = true)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 24)
public class APIRecoverHostMemoryOptimizationMsg extends APICreateMessage {
    @APIParam(resourceType = HostVO.class)
    private String uuid;
    @APIParam
    private String expectedControlOperationUuid;
    @APIParam
    private String expectedHostBootId;
    @APIParam
    private String expectedPoolGeneration;
    @APIParam(required = false)
    private String drainControlOperationUuid;

    public String getUuid() { return uuid; }
    public void setUuid(String value) { uuid = value; }
    public String getExpectedControlOperationUuid() { return expectedControlOperationUuid; }
    public void setExpectedControlOperationUuid(String value) { expectedControlOperationUuid = value; }
    public String getExpectedHostBootId() { return expectedHostBootId; }
    public void setExpectedHostBootId(String value) { expectedHostBootId = value; }
    public String getExpectedPoolGeneration() { return expectedPoolGeneration; }
    public void setExpectedPoolGeneration(String value) { expectedPoolGeneration = value; }
    public String getDrainControlOperationUuid() { return drainControlOperationUuid; }
    public void setDrainControlOperationUuid(String value) { drainControlOperationUuid = value; }

    MemoryUncertainRecovery toRecovery() {
        MemoryUncertainRecovery recovery = new MemoryUncertainRecovery();
        recovery.setExpectedHostBootId(expectedHostBootId);
        recovery.setExpectedPoolGeneration(expectedPoolGeneration);
        recovery.setDrainControlOperationUuid(drainControlOperationUuid);
        recovery.setConfirmed(true);
        return recovery;
    }

    public static APIRecoverHostMemoryOptimizationMsg __example__() {
        APIRecoverHostMemoryOptimizationMsg msg = new APIRecoverHostMemoryOptimizationMsg();
        msg.setUuid(uuid());
        msg.setExpectedControlOperationUuid(uuid());
        msg.setExpectedHostBootId("d339587c-b015-4745-8091-d00521319861");
        msg.setExpectedPoolGeneration("ffc64a56d42b81a52b3858d327ec72aeeef3b8144d468477f32dc284eb28238d");
        msg.setDrainControlOperationUuid(uuid());
        return msg;
    }
}
