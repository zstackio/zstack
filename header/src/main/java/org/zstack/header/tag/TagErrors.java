package org.zstack.header.tag;

/**
 */
public enum TagErrors {
    DUPLICATED_TAG(1000);

    private String code;

    TagErrors(int id) {
        code = String.format("TAG.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
