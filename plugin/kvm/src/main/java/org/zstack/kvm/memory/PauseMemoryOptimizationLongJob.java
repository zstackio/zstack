package org.zstack.kvm.memory;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.UseApiTimeout;

@UseApiTimeout(APIPauseMemoryOptimizationMsg.class)
@LongJobFor(APIPauseMemoryOptimizationMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PauseMemoryOptimizationLongJob extends MemoryClusterOperationLongJob {
    public PauseMemoryOptimizationLongJob() { super(APIPauseMemoryOptimizationMsg.class, "pause"); }
}
