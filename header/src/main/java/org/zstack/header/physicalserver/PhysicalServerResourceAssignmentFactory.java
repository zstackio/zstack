package org.zstack.header.physicalserver;

import java.util.Collection;
import java.util.function.BiConsumer;
import org.zstack.header.core.Completion;

public interface PhysicalServerResourceAssignmentFactory {
    PhysicalServerRoleType getRoleType();

    RoleServiceManifest roleServices();

    PhysicalServerResourceAssignmentObserver getResourceAssignment(String serverUuid);

    default void initializeAssociations(int batchSize, BiConsumer<Collection<String>, Completion> batch,
                                        Completion completion) {
        completion.success();
    }
}
