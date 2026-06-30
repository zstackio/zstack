package org.zstack.header.scim;

public class ScimResult {
    public String clientId;
    public String eventId;
    public String resourceType;
    public String resourceId;
    public String operation;
    public boolean duplicate;

    public static ScimResult applied(String clientId, String eventId, String resourceType, String resourceId,
                                     String operation) {
        ScimResult result = new ScimResult();
        result.clientId = clientId;
        result.eventId = eventId;
        result.resourceType = resourceType;
        result.resourceId = resourceId;
        result.operation = operation;
        return result;
    }

    public static ScimResult duplicate(String clientId, String eventId, String resourceType, String resourceId) {
        ScimResult result = applied(clientId, eventId, resourceType, resourceId, "DUPLICATE");
        result.duplicate = true;
        return result;
    }
}
