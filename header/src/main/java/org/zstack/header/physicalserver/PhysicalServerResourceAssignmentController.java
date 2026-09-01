package org.zstack.header.physicalserver;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public interface PhysicalServerResourceAssignmentController extends
        PhysicalServerResourceAssignmentObserver {
    PhysicalServerResourceIsolationMode getIsolationMode();

    default String getTopologyRoleType() {
        return getRoleType();
    }

    default String getDefaultCpuSet(
            PhysicalServerCpuTopology topology,
            Set<Integer> allocatedExclusiveCpus) {
        Set<Integer> available = new TreeSet<>(topology.getOnlineCpus());
        available.removeAll(allocatedExclusiveCpus);
        return PhysicalServerCpuSet.format(available);
    }

    List<ResourceConsumerHandle> getResourceConsumers(String serverUuid);

    void collectTopology(
            String serverUuid,
            ReturnValueCompletion<PhysicalServerCpuTopology> completion);

    void apply(
            String serverUuid,
            String consumerUuid,
            ResourceControlCommand command,
            ReturnValueCompletion<ResourceControlResponse> completion);

    void restartManagedServices(
            String serverUuid,
            Collection<String> serviceNames,
            Completion completion);
}
