package org.zstack.kvm.memory;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;

import java.util.concurrent.TimeUnit;

/** Pauses memory optimization on the KVM Hosts of a Cluster, or on one of them, as a LongJob. */
@Action(category = "memoryOptimization", adminOnly = true)
@RestRequest(path = "/clusters/{uuid}/actions", method = HttpMethod.PUT,
        responseClass = APIPauseMemoryOptimizationEvent.class, isAction = true)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 24)
public class APIPauseMemoryOptimizationMsg extends APIMemoryClusterOperationMsg {
    public static APIPauseMemoryOptimizationMsg __example__() {
        APIPauseMemoryOptimizationMsg msg = new APIPauseMemoryOptimizationMsg();
        msg.setUuid(uuid());
        msg.setHostUuid(uuid());
        return msg;
    }
}
