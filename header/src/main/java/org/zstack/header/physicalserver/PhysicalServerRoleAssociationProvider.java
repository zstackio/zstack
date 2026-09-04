package org.zstack.header.physicalserver;

import java.util.Collection;
import java.util.Set;

public interface PhysicalServerRoleAssociationProvider {
    /**
     * Returns the Role type provided by this extension.
     */
    PhysicalServerRoleType getRoleType();

    /**
     * Discovers the complete Role association view in the requested scope.
     *
     * @param serverUuids {@code null} or empty to discover all PhysicalServers;
     *                    otherwise the exact PhysicalServer scope to refresh
     * @return a non-null set; for a non-empty scope every returned UUID must
     *         belong to {@code serverUuids}
     * @throws RuntimeException when a complete view cannot be produced; the
     *                          caller then retains the previous associations
     */
    Set<String> discoverAssociations(Collection<String> serverUuids);
}
