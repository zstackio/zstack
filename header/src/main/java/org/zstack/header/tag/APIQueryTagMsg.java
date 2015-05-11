package org.zstack.header.tag;

import org.zstack.header.query.APIQueryMessage;

/**
 */
public class APIQueryTagMsg extends APIQueryMessage {
    private boolean systemTag;

    public boolean isSystemTag() {
        return systemTag;
    }

    public void setSystemTag(boolean systemTag) {
        this.systemTag = systemTag;
    }
}
