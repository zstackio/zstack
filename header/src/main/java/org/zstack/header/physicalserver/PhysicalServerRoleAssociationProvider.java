package org.zstack.header.physicalserver;

import java.util.Collection;
import java.util.Set;

public interface PhysicalServerRoleAssociationProvider {
    String getRoleType();

    Set<String> refreshAssociations(Collection<String> serverUuids);
}
