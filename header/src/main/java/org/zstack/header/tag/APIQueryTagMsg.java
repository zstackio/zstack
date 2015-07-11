package org.zstack.header.tag;

import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;

/**
 */
@Action(category = TagConstant.ACTION_CATEGORY, names = {"read"})
public class APIQueryTagMsg extends APIQueryMessage {
    private boolean systemTag;

    public boolean isSystemTag() {
        return systemTag;
    }

    public void setSystemTag(boolean systemTag) {
        this.systemTag = systemTag;
    }
}
