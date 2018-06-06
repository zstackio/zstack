package org.zstack.identity;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;

public interface APIResponseFilter {
    Message filter(APIMessage req, Message rsp);
}
