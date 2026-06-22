package org.zstack.header.scim;

public interface ScimResourceHandler {
    String normalizeResourceType(String resourceType);

    void applyResource(ScimOperation operation, String resourceType, String resourceId, ScimPayload payload);
}
