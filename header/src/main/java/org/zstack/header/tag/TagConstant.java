package org.zstack.header.tag;

/**
 */
public interface TagConstant {
    String SERVICE_ID = "tag";

    String ACTION_CATEGORY = "tag";

    String EPHEMERAL_TAG_PREFIX = "ephemeral";

    static boolean isEphemeralTag(String tag) {
        return tag.startsWith(String.format("%s::", EPHEMERAL_TAG_PREFIX));
    }
}
