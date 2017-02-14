package org.zstack.header.tag;

import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;

import java.util.List;

import static java.util.Arrays.asList;

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

    public static List<String> __example__() {
        return asList();
    }

}
