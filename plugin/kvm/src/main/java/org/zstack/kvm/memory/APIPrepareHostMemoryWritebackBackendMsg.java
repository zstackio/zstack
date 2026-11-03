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
 * as a LongJob. The management node checks the candidate against the Host just before the reset.
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

    public String getUuid() { return uuid; }
    public void setUuid(String value) { uuid = value; }
    public String getCandidateId() { return candidateId; }
    public void setCandidateId(String value) { candidateId = value; }

    public static APIPrepareHostMemoryWritebackBackendMsg __example__() {
        APIPrepareHostMemoryWritebackBackendMsg msg = new APIPrepareHostMemoryWritebackBackendMsg();
        msg.setUuid(uuid());
        msg.setCandidateId("candidate:ddc10b8f02dc512667056d6d7c6fb8cdb13701fdb96ef373f62619a053b01541");
        return msg;
    }
}
