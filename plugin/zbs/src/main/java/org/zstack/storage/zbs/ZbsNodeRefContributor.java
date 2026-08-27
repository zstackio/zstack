package org.zstack.storage.zbs;

import java.util.Collection;
import java.util.Map;

public interface ZbsNodeRefContributor {
    /**
     * Lists ZBS relations keyed by PhysicalServer UUID.
     *
     * @param serverUuids PhysicalServers to select; {@code null} or empty
     *                    selects all relations
     * @return a non-null map keyed by PhysicalServer UUID
     */
    Map<String, ZbsNodeRef> bulkList(Collection<String> serverUuids);
}
