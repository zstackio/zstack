package org.zstack.identity.rbac;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.identity.APIResponseFilter;

public class RBACAPIResponseFilter implements APIResponseFilter {
    @Override
    public Message filter(APIMessage req, Message rsp) {
        return rsp;
    }
}
