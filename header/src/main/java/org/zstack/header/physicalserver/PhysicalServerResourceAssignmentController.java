package org.zstack.header.physicalserver;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;

import java.util.Collection;
import java.util.List;

public interface PhysicalServerResourceAssignmentController extends PhysicalServerResourceAssignmentObserver {
    PhysicalServerResourceIsolationMode getIsolationMode();

    Integer getDefaultCpuCount();

    List<ResourceConsumerHandle> getResourceConsumers(String serverUuid);

    void collectTopology(String serverUuid, ReturnValueCompletion<PhysicalServerCpuTopology> completion);

    void apply(String serverUuid, ResourceControlCommand command, ReturnValueCompletion<Boolean> completion);

    void release(String serverUuid, ResourceControlCommand command, ReturnValueCompletion<Boolean> completion);

    void restartManagedServices(String serverUuid, Collection<ResourceConsumerHandle> consumers, Completion completion);
}
