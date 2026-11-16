package org.zstack.header.physicalserver;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import java.util.List;

public interface PhysicalServerResourceAssignmentController extends PhysicalServerResourceAssignmentObserver {
    @Override
    default void collectResourceAssignment(
            String serverUuid, List<String> serviceNames,
            ReturnValueCompletion<PhysicalServerResourceBoundary> completion) {
        throw new UnsupportedOperationException("Writable Assignment state is reported by Apply");
    }

    void collectTopology(String serverUuid, ReturnValueCompletion<PhysicalServerCpuTopology> completion);

    void apply(String serverUuid, ResourceControlCommand command, ReturnValueCompletion<Boolean> completion);

    void release(String serverUuid, ResourceControlCommand command, ReturnValueCompletion<Boolean> completion);

    void restartManagedServices(String serverUuid, ResourceControlCommand command, Completion completion);
}
