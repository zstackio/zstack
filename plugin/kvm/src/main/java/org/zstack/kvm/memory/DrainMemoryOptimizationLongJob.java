package org.zstack.kvm.memory;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.UseApiTimeout;

@UseApiTimeout(APIDrainMemoryOptimizationMsg.class)
@LongJobFor(APIDrainMemoryOptimizationMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DrainMemoryOptimizationLongJob extends MemoryClusterOperationLongJob {
    public DrainMemoryOptimizationLongJob() { super(APIDrainMemoryOptimizationMsg.class, "drain"); }
}
