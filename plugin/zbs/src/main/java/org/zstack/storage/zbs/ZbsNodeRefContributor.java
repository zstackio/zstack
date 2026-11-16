package org.zstack.storage.zbs;

import java.util.Collection;
import java.util.Map;

public interface ZbsNodeRefContributor {
    Map<String, ZbsNodeRef> getNodesByServerUuids(Collection<String> serverUuids);

    Map<String, ZbsNodeRef> getAllNodesBySerialNumber();
}
