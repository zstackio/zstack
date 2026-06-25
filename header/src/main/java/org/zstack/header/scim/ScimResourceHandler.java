package org.zstack.header.scim;

public interface ScimResourceHandler {
    String normalizeResourceType(String resourceType);

    void applyResource(ScimOperation operation, String resourceType, String resourceId, ScimPayload payload);

    /**
     * Remove resources that were created for the given synchronization type.
     *
     * @param syncType synchronization type marker, for example SCIM
     */
    void cleanupResources(String syncType);
}
