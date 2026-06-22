package org.zstack.header.scim;

public class ScimResult {
    public String clientId;
    public String eventId;
    public String resourceType;
    public String resourceId;
    public long resourceVersion;
    public String operation;
    public boolean duplicate;

    public static ScimResult applied(String clientId, String eventId, String resourceType, String resourceId,
                                     long version, String operation) {
        ScimResult result = new ScimResult();
        result.clientId = clientId;
        result.eventId = eventId;
        result.resourceType = resourceType;
        result.resourceId = resourceId;
        result.resourceVersion = version;
        result.operation = operation;
        return result;
    }

    public static ScimResult duplicate(String clientId, String eventId, String resourceType, String resourceId,
                                       long version) {
        ScimResult result = applied(clientId, eventId, resourceType, resourceId, version, "DUPLICATE");
        result.duplicate = true;
        return result;
    }
}
