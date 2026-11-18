package org.zstack.header.identity;

import org.zstack.header.message.APIMessage;

import java.util.List;
import java.util.Map;

public interface APIResourceReferenceExtensionPoint {
    // Additional resources used by a write API, checked after its operation targets.
    Map<Class<?>, List<String>> getResourceReferences(APIMessage msg);
}
