package org.zstack.header.scim;

public class ScimConstant {
    public static final String HEADER_CLIENT_ID = "X-SCIM-Client-UUID";
    public static final String HEADER_EVENT_ID = "X-SCIM-Event-UUID";
    public static final String HEADER_TIMESTAMP = "X-SCIM-Timestamp";
    public static final String HEADER_SIGNATURE = "X-SCIM-Signature";
    public static final String DEFAULT_CLIENT_ID = "default";
    public static final String EVENT_STATUS_PENDING = "PENDING";
    public static final String EVENT_STATUS_APPLIED = "APPLIED";
    public static final String EVENT_STATUS_FAILED = "FAILED";

    private ScimConstant() {
    }
}
