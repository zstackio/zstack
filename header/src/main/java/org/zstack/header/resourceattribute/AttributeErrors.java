package org.zstack.header.resourceattribute;

public enum AttributeErrors {
    GENERIC_ERROR(1000),
    DUPLICATED_ATTRIBUTE(1001),
    UNSUPPORTED_RESOURCE_TYPE(1002),
    REMOVE_RESOURCE_TYPE_NOT_ALLOWED(1003),
    ;

    private String code;

    AttributeErrors(int id) {
        code = String.format("ATTRIBUTE.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
