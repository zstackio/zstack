package org.zstack.header.resource;

/**
 * Constants shared by resource source tracking. SCIM receivers use these values
 * to mark resources owned by upstream identity providers such as ZIAM.
 */
public class ResourceSourceConstant {
    public static final String SOURCE_CATEGORY_LOCAL = "LOCAL";
    public static final String SOURCE_CATEGORY_UNIFIED_AUTH = "UNIFIED_AUTH";
    public static final String SOURCE_CATEGORY_ZIAM_SCIM = "ZIAM_SCIM";
    public static final String SOURCE_TYPE_ZIAM = "ZIAM";
    public static final String SOURCE_NAME_ZIAM = "ziam";
    public static final String SYNC_TYPE_SCIM = "SCIM";

    private ResourceSourceConstant() {
    }
}
