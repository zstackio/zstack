package org.zstack.kvm.memory;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.longjob.LongJobFor;
import org.zstack.header.longjob.UseApiTimeout;

@UseApiTimeout(APIResumeMemoryOptimizationMsg.class)
@LongJobFor(APIResumeMemoryOptimizationMsg.class)
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ResumeMemoryOptimizationLongJob extends MemoryClusterOperationLongJob {
    public ResumeMemoryOptimizationLongJob() { super(APIResumeMemoryOptimizationMsg.class, "resume"); }
}
