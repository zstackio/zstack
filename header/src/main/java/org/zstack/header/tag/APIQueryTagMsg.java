package org.zstack.header.tag;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.rest.RestRequest;

/**
 */
@Action(category = TagConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/tags",
        optionalPaths = {"/tags/{uuid}"},
        responseClass = APIQueryTagReply.class,
        method = HttpMethod.GET
)
public class APIQueryTagMsg extends APIQueryMessage {
    private boolean systemTag;

    public boolean isSystemTag() {
        return systemTag;
    }

    public void setSystemTag(boolean systemTag) {
        this.systemTag = systemTag;
    }
}
